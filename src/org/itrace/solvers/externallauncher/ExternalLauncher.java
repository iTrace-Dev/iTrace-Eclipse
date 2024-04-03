package org.itrace.solvers.externallauncher;

import org.itrace.gaze.IGazeResponse;

import java.io.IOException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import org.itrace.solvers.externallauncher.IExternalLauncher;

public class ExternalLauncher implements IExternalLauncher, EventHandler {
    private String filenameSuffix = "-responses-USERNAME-yyMMddTHHmmss-SSSS-Z.csv";
	private String sessionID;
	
	private Process affectivaProcess = null;
	private Process shimmerProcess = null;
    private Process empaticaProcess = null;
    
    @Override
    public void config(String sessionID, String devUsername) {
    	filenameSuffix = "-responses-" + devUsername + "-" + sessionID + ".csv";
    	this.sessionID = sessionID;
    }
    public void config(String sessionID) {
    	filenameSuffix = "-responses-" + sessionID + ".csv";
    	this.sessionID = sessionID;
    }
    
    public String getFilename(String prefix) {
        String workspaceLocation = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString();
        //return workspaceLocation + "/" + sessionID + "/" + prefix + filenameSuffix;\
        System.out.println( workspaceLocation + "/" + prefix + filenameSuffix);
        return workspaceLocation + "/" + prefix + filenameSuffix;
    }
    
    public void displayExportFile() {
    	// Do nothing
        // TODO: Potentially split this into two classes so that we can support this?
    }
    
    public void dispose() {
    	if(this.shimmerProcess != null) {
    		this.shimmerProcess = null;
    	}
    	if(this.affectivaProcess != null) {
    		this.affectivaProcess = null;
    	}
        if(this.empaticaProcess != null){
            this.empaticaProcess = null;
        }
    }

    public String friendlyName() {
        return "External Application Launcher";
    }
    
    public void init() {
        this.start();
    }
    
    public void process(IGazeResponse response) {
        // Do nothing
    }
    
	public void handleEvent(Event event) {
		// Do nothing
	}
    
    public void start() {
        System.out.println("Launching external applications.");
        	
        // TODO: Make these paths configurable.
        
        try {
        	//affectivaProcess = new ProcessBuilder("cmd", "/k", "start", "C:\\EmotionalAwareness-Environment\\iTrace-Archive-SessionTimeServ\\iTraceAffectivaPortTest.exe", getFilename("affectiva")).start();
        	affectivaProcess = new ProcessBuilder("cmd", "/k", "start", "C:\\EmotionalAwareness-Environment\\Archive-to-Master-Port\\iTrace-Archive-Affectiva\\Affectiva\\bin\\x64\\Debug\\Affectiva.exe", getFilename("affectiva")).start();
        } catch(IOException e) {
			e.printStackTrace();
		}
        
        // try {
	    //     shimmerProcess = new ProcessBuilder("C:\\EmotionalAwareness-Environment\\iTrace-Archive-SessionTimeServ\\iTraceShimmerCapturePortTest.exe", getFilename("shimmer")).start();
		// } catch(IOException e) {
		// 	e.printStackTrace();
		// }

        try {
        	empaticaProcess = new ProcessBuilder("cmd", "/k", "start", "C:\\EmotionalAwareness-Environment\\Archive-to-Master-Port\\ble-client-windows-master\\bin\\Debug\\EmpaticaBLEClient.exe", getFilename("empatica")).start();
        } catch(IOException e) {
			e.printStackTrace();
		}
    }
}
