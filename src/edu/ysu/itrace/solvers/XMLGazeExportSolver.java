package edu.ysu.itrace.solvers;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import edu.ysu.itrace.gaze.IGazeResponse;
import edu.ysu.itrace.gaze.IStyledTextGazeResponse;

/**
 * Solver that simply dumps gaze data to disk in XML format.
 */
public class XMLGazeExportSolver implements IFileExportSolver, EventHandler {
    private static final String EOL = System.getProperty("line.separator");
    private XMLOutputFactory outFactory = XMLOutputFactory.newInstance();
    private XMLStreamWriter responseWriter;
    private File outFile;
    private String filename = "";
    private Dimension screenRect;
    public boolean initialized = false;

    public XMLGazeExportSolver() {
    	UIManager.put("swing.boldMetal", new Boolean(false)); //make UI font plain
    }
    
    @Override
    public void init() {
    	initialized = true;
        screenRect = Toolkit.getDefaultToolkit().getScreenSize();
        try {
        	outFile = new File(getFilename());
            responseWriter = outFactory.createXMLStreamWriter(new FileOutputStream(outFile), "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException("Log files could not be created: " + e.getMessage());
        } catch (XMLStreamException e) {
            throw new RuntimeException("Log files could not be created: " + e.getMessage());
        }
        System.out.println("Putting files at " + outFile.getAbsolutePath());

        try {
        	// Setup start of XML doc (UTF-8 and version 1.0)
        	responseWriter.writeStartDocument("utf-8", "1.0");
            responseWriter.writeCharacters(EOL);
            
            // Show that data is from a plugin
            responseWriter.writeStartElement("plugin");
            responseWriter.writeCharacters(EOL);
            
            // Record environment data
            responseWriter.writeStartElement("environment");
            responseWriter.writeCharacters(EOL);
            
            // Screen Dimensions
            responseWriter.writeEmptyElement("screen-size");
            responseWriter.writeAttribute("width",
                    String.valueOf(screenRect.width));
            responseWriter.writeAttribute("height",
                    String.valueOf(screenRect.height));
            responseWriter.writeCharacters(EOL);
            
            // Plugin Type
            responseWriter.writeEmptyElement("application");
            responseWriter.writeAttribute("type", "eclipse");
            responseWriter.writeCharacters(EOL);
            
            // End Environment
            responseWriter.writeEndElement();
            responseWriter.writeCharacters(EOL);
            
            // Start Gaze Data Section
            responseWriter.writeStartElement("gazes");
            responseWriter.writeCharacters(EOL);
            
        } catch (Exception e) {
            throw new RuntimeException("Log file header could not be written: "
                    + e.getMessage());
        }
    }

    @Override
    public void process(IGazeResponse response) {
        try {                
			responseWriter.writeStartElement("response");
			responseWriter.writeAttribute("object_name", response.getName());
			responseWriter.writeAttribute("type", response.getGazeType());
			responseWriter.writeAttribute("x", String.valueOf(response.getGaze().getX()));
			responseWriter.writeAttribute("y", String.valueOf(response.getGaze().getY()));
			responseWriter.writeAttribute("timestamp", String.valueOf(response.getGaze().getTimestamp()));
			responseWriter.writeAttribute("event_time", String.valueOf(response.getGaze().getEventTime()));
			
			if (response instanceof IStyledTextGazeResponse) {
			    IStyledTextGazeResponse styledResponse = (IStyledTextGazeResponse) response;
			    responseWriter.writeAttribute("path", styledResponse.getPath());
			    responseWriter.writeAttribute("line_height", String.valueOf(styledResponse.getLineHeight()));
			    responseWriter.writeAttribute("font_height", String.valueOf(styledResponse.getFontHeight()));
			    responseWriter.writeAttribute("line", String.valueOf(styledResponse.getLine()));
			    responseWriter.writeAttribute("col", String.valueOf(styledResponse.getCol()));
			    responseWriter.writeAttribute("line_base_x", String.valueOf(styledResponse.getLineBaseX()));
			    responseWriter.writeAttribute("line_base_y", String.valueOf(styledResponse.getLineBaseY()));
			} 
			
			responseWriter.writeEndElement();
			responseWriter.writeCharacters(EOL);        
        } catch (XMLStreamException e) { /* ignore write errors */ }
    }

    @Override
    public void dispose() {
        try {
            responseWriter.writeEndElement();
            responseWriter.writeCharacters(EOL);
            responseWriter.writeEndElement();
            responseWriter.writeCharacters(EOL);
            responseWriter.writeEndDocument();
            responseWriter.writeCharacters(EOL);
            responseWriter.flush();
            responseWriter.close();
            System.out.println("Gaze responses saved.");
        } catch (XMLStreamException e) {
            throw new RuntimeException("Log file footer could not be written: "
                    + e.getMessage());
        }
        outFile = null;
    }
    
    @Override
    public void config(String dirLocation) {
    	filename = dirLocation;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public String friendlyName() {
        return "XML Gaze Export";
    }

    @Override
    public void displayExportFile() {
    	JTextField displayVal = new JTextField(filename);
    	displayVal.setEditable(false);
    	
    	JPanel displayPanel = new JPanel();
    	displayPanel.setLayout(new BoxLayout(displayPanel, BoxLayout.Y_AXIS)); //vertically align
    	displayPanel.add(new JLabel("Export Filename"));
    	displayPanel.add(displayVal);
    	displayPanel.setPreferredSize(new Dimension(400,40)); //resize appropriately
    	
    	final int displayDialog = JOptionPane.showConfirmDialog(null, displayPanel, 
    			friendlyName() + " Display", JOptionPane.OK_CANCEL_OPTION,
    			JOptionPane.PLAIN_MESSAGE);
    	if (displayDialog == JOptionPane.OK_OPTION) {
    		//do nothing
    	}
    }

	@Override
	public void handleEvent(Event event) {
		if(outFile == null) this.init();
		String[] propertyNames = event.getPropertyNames();
		IGazeResponse response = (IGazeResponse)event.getProperty(propertyNames[0]);
		this.process(response);
		
	}
}
