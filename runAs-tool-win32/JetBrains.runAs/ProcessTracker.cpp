#include "stdafx.h"
#include "ProcessTracker.h"
#include <iostream>
#include "ErrorUtilities.h"
#include "IProcess.h"
#include <thread>
#include <chrono>

ProcessTracker::ProcessTracker(SECURITY_ATTRIBUTES& securityAttributes, STARTUPINFO& startupInfo)
{
	_stdOutPipe.Initialize(securityAttributes);
	_stdErrorOutPipe.Initialize(securityAttributes);
	_stdInPipe.Initialize(securityAttributes);

	startupInfo.hStdOutput = _stdOutPipe.GetWriter().Value();
	startupInfo.hStdError = _stdErrorOutPipe.GetWriter().Value();
	startupInfo.hStdInput = _stdInPipe.GetReader().Value();
	startupInfo.dwFlags |= STARTF_USESTDHANDLES;
	
}

DWORD ProcessTracker::WaiteForExit(HANDLE processHandle)
{
	DWORD exitCode;
	auto hStdOutput = GetStdHandle(STD_OUTPUT_HANDLE);
	auto hStdError = GetStdHandle(STD_ERROR_HANDLE);
	bool hasData;
	do
	{
		std::this_thread::sleep_for(std::chrono::milliseconds(0));
		hasData = RedirectStream(_stdOutPipe.GetReader().Value(), hStdOutput) || RedirectStream(_stdErrorOutPipe.GetReader().Value(), hStdError);		
		if (!GetExitCodeProcess(processHandle, &exitCode))
		{
			std::wcerr << ErrorUtilities::GetLastErrorMessage(L"GetExitCodeProcess");
			return IProcess::ErrorExitCode;
		}
	}
	while (exitCode == STILL_ACTIVE || hasData);

	return exitCode;
}

bool ProcessTracker::RedirectStream(HANDLE hPipeRead, HANDLE hOutput)
{
	CHAR buffer[0xffff];
	DWORD bytesReaded;
	DWORD bytesWritten;
	DWORD totalBytesAvail;
	DWORD bytesLeftThisMessage;

	if (!PeekNamedPipe(hPipeRead, buffer, sizeof(buffer), &bytesReaded, &totalBytesAvail, &bytesLeftThisMessage))
	{
		if (GetLastError() == ERROR_BROKEN_PIPE)
		{
			// Pipe done - normal exit path.
			return true;
		}

		std::wcerr << ErrorUtilities::GetLastErrorMessage(L"PeekNamedPipe");
		return false;
	}

	if (totalBytesAvail == 0)
	{
		return false;
	}

	if (!ReadFile(hPipeRead, buffer, bytesReaded, &bytesReaded, nullptr) || !bytesReaded)
	{
		if (GetLastError() == ERROR_BROKEN_PIPE)
		{
			// Pipe done - normal exit path.
			return false;
		}

		std::wcerr << ErrorUtilities::GetLastErrorMessage(L"ReadFile");
		return false;
	}

	if (!WriteFile(hOutput, buffer, bytesReaded, &bytesWritten, nullptr))
	{
		std::wcerr << ErrorUtilities::GetLastErrorMessage(L"WriteConsole");
		return false;
	}

	return true;
}

