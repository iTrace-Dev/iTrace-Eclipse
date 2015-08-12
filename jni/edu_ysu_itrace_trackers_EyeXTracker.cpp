#include <Windows.h>
#include <jni.h>
#include <stdio.h>
#include <conio.h>
#include <assert.h>
#include "edu_ysu_itrace_trackers_EyeXTracker.h"
#include "eyex/EyeX.h"

#pragma comment (lib, "Tobii.EyeX.Client.lib")

#ifdef __cplusplus
extern "C" {
#endif

static const TX_STRING InteractorId = "iTrace";

// global variables
static TX_HANDLE g_hGlobalInteractorSnapshot = TX_EMPTY_HANDLE;

JavaVM * g_vm;
jobject g_obj;
jmethodID g_mid;
bool alreadyStreamed = false;
TX_CONTEXTHANDLE hContext;

/*
* Initializes g_hGlobalInteractorSnapshot with an interactor that has the Gaze Point behavior.
*/
BOOL InitializeGlobalInteractorSnapshot(TX_CONTEXTHANDLE hContext)
{
	TX_HANDLE hInteractor = TX_EMPTY_HANDLE;
	TX_HANDLE hBehavior = TX_EMPTY_HANDLE;
	TX_GAZEPOINTDATAPARAMS params = { TX_GAZEPOINTDATAMODE_LIGHTLYFILTERED };
	BOOL success;

	success = txCreateGlobalInteractorSnapshot(
		hContext,
		InteractorId,
		&g_hGlobalInteractorSnapshot,
		&hInteractor) == TX_RESULT_OK;
	success &= txCreateInteractorBehavior(hInteractor, &hBehavior, TX_INTERACTIONBEHAVIORTYPE_GAZEPOINTDATA) == TX_RESULT_OK;
	success &= txSetGazePointDataBehaviorParams(hBehavior, &params) == TX_RESULT_OK;

	txReleaseObject(&hBehavior);
	txReleaseObject(&hInteractor);

	return success;
}

/*
* Callback function invoked when a snapshot has been committed.
*/
void TX_CALLCONVENTION OnSnapshotCommitted(TX_CONSTHANDLE hAsyncData, TX_USERPARAM param)
{
	// check the result code using an assertion.
	// this will catch validation errors and runtime errors in debug builds. in release builds it won't do anything.

	TX_RESULT result = TX_RESULT_UNKNOWN;
	txGetAsyncDataResultCode(hAsyncData, &result);
	assert(result == TX_RESULT_OK || result == TX_RESULT_CANCELLED);
}

/*
* Callback function invoked when the status of the connection to the EyeX Engine has changed.
*/
void TX_CALLCONVENTION OnEngineConnectionStateChanged(TX_CONNECTIONSTATE connectionState, TX_USERPARAM userParam)
{
	switch (connectionState) {
	case TX_CONNECTIONSTATE_CONNECTED: {
		BOOL success;
		printf("The connection state is now CONNECTED (We are connected to the EyeX Engine)\n");
		// commit the snapshot with the global interactor as soon as the connection to the engine is established.
		// (it cannot be done earlier because committing means "send to the engine".)
		success = txCommitSnapshotAsync(g_hGlobalInteractorSnapshot, OnSnapshotCommitted, NULL) == TX_RESULT_OK;
		if (!success) {
			printf("Failed to initialize the data stream.\n");
		}
		else
		{
			printf("Waiting for gaze data to start streaming...\n");
		}
	}
		break;

	case TX_CONNECTIONSTATE_DISCONNECTED:
		printf("The connection state is now DISCONNECTED (We are disconnected from the EyeX Engine)\n");
		break;

	case TX_CONNECTIONSTATE_TRYINGTOCONNECT:
		printf("The connection state is now TRYINGTOCONNECT (We are trying to connect to the EyeX Engine)\n");
		break;

	case TX_CONNECTIONSTATE_SERVERVERSIONTOOLOW:
		printf("The connection state is now SERVER_VERSION_TOO_LOW: this application requires a more recent version of the EyeX Engine to run.\n");
		break;

	case TX_CONNECTIONSTATE_SERVERVERSIONTOOHIGH:
		printf("The connection state is now SERVER_VERSION_TOO_HIGH: this application requires an older version of the EyeX Engine to run.\n");
		break;
	}
}

/*
* Calls a Java method to notify of new events
*/
void callback(double x, double y, long long timestamp) {
	JNIEnv * g_env;
	// double check it's all ok
	int getEnvStat = g_vm->GetEnv((void **)&g_env, JNI_VERSION_1_4);
	if (getEnvStat == JNI_EDETACHED) {
		//Do nothing
		if (g_vm->AttachCurrentThread((void **)&g_env, NULL) != 0) {
		//Do nothing
		}
	}
	else if (getEnvStat == JNI_OK) {
		//
	}
	else if (getEnvStat == JNI_EVERSION) {
		printf("GetEnv: version not supported");
	}

	g_env->CallVoidMethod(g_obj, g_mid, x, y, timestamp);

	if (g_env->ExceptionCheck()) {
		g_env->ExceptionDescribe();
	}

	g_vm->DetachCurrentThread();
}

/*
* Handles an event from the Gaze Point data stream.
*/
void OnGazeDataEvent(TX_HANDLE hGazeDataBehavior)
{
	TX_GAZEPOINTDATAEVENTPARAMS eventParams;
	if (txGetGazePointDataEventParams(hGazeDataBehavior, &eventParams) == TX_RESULT_OK) {
		//printf("Gaze Data: (%.1f, %.1f) timestamp %.0f ms\n", eventParams.X, eventParams.Y, eventParams.Timestamp);
		if (!alreadyStreamed) {
			alreadyStreamed = true;
			printf("Start streaming");
		}
		callback(eventParams.X, eventParams.Y, eventParams.Timestamp);
	}
	else {
		printf("Failed to interpret gaze data event packet.\n");
	}
}

/*
* Callback function invoked when an event has been received from the EyeX Engine.
*/
void TX_CALLCONVENTION HandleEvent(TX_CONSTHANDLE hAsyncData, TX_USERPARAM userParam)
{
	TX_HANDLE hEvent = TX_EMPTY_HANDLE;
	TX_HANDLE hBehavior = TX_EMPTY_HANDLE;

	txGetAsyncDataContent(hAsyncData, &hEvent);

	// NOTE. Uncomment the following line of code to view the event object. The same function can be used with any interaction object.
	//OutputDebugStringA(txDebugObject(hEvent));

	if (txGetEventBehavior(hEvent, &hBehavior, TX_INTERACTIONBEHAVIORTYPE_GAZEPOINTDATA) == TX_RESULT_OK) {
		OnGazeDataEvent(hBehavior);
		txReleaseObject(&hBehavior);
	}

	// NOTE since this is a very simple application with a single interactor and a single data stream, 
	// our event handling code can be very simple too. A more complex application would typically have to 
	// check for multiple behaviors and route events based on interactor IDs.

	txReleaseObject(&hEvent);
}

JNIEXPORT jboolean JNICALL
Java_edu_ysu_itrace_trackers_EyeXTracker_register(JNIEnv * env, jobject obj) {
	bool returnValue = true;
	// convert local to global reference 
	// (local will die after this method call)
	g_obj = env->NewGlobalRef(obj);

	env->GetJavaVM(&g_vm);

	// save refs for callback
	jclass g_clazz = env->GetObjectClass(g_obj);
	if (g_clazz == NULL) {
		printf("Failed to find class");
	}

	g_mid = env->GetMethodID(g_clazz, "callback", "(DDJ)V");
	if (g_mid == NULL) {
		printf("Unable to get method ref");
	}
	return (jboolean)returnValue;
}


JNIEXPORT jboolean JNICALL
Java_edu_ysu_itrace_trackers_EyeXTracker_connectEyeTracker(JNIEnv * env, jobject obj)
{
	hContext = TX_EMPTY_HANDLE;
	TX_TICKET hConnectionStateChangedTicket = TX_INVALID_TICKET;
	TX_TICKET hEventHandlerTicket = TX_INVALID_TICKET;
	BOOL success;

	// initialize and enable the context that is our link to the EyeX Engine.
	success = txInitializeSystem(TX_SYSTEMCOMPONENTOVERRIDEFLAG_NONE, NULL, NULL, NULL) == TX_RESULT_OK;
	success &= txCreateContext(&hContext, TX_FALSE) == TX_RESULT_OK;
	success &= InitializeGlobalInteractorSnapshot(hContext);
	success &= txRegisterConnectionStateChangedHandler(hContext, &hConnectionStateChangedTicket, OnEngineConnectionStateChanged, NULL) == TX_RESULT_OK;
	success &= txRegisterEventHandler(hContext, &hEventHandlerTicket, HandleEvent, NULL) == TX_RESULT_OK;
	success &= txEnableConnection(hContext) == TX_RESULT_OK;

	// let the events flow until a key is pressed.
	if (success) {
		printf("Initialization was successful.\n");
	} else {
		printf("Initialization failed.\n");
	}
	return success;
}

JNIEXPORT void JNICALL
Java_edu_ysu_itrace_trackers_EyeXTracker_disconnectEyeTracker(JNIEnv *env, jobject obj)
{
	printf("Disconnect");
	txDisableConnection(hContext);
	txReleaseObject(&g_hGlobalInteractorSnapshot);
	txShutdownContext(hContext, TX_CLEANUPTIMEOUT_DEFAULT, TX_FALSE);
	txReleaseContext(&hContext);
	return;
}

#ifdef __cplusplus
}
#endif