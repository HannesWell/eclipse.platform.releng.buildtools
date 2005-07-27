/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.test.performance.ui;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.test.internal.performance.PerformanceTestPlugin;
import org.eclipse.test.internal.performance.data.Dim;
import org.eclipse.test.internal.performance.db.DB;
import org.eclipse.test.internal.performance.db.Scenario;
import org.eclipse.test.internal.performance.db.SummaryEntry;
import org.eclipse.test.internal.performance.db.TimeSeries;
import org.eclipse.test.internal.performance.db.Variations;
import org.eclipse.test.performance.ui.Utils.ConfigDescriptor;


public class FingerPrint {
    
    private static final int GRAPH_WIDTH= 1000;

    String outputDirectory;
    String referenceBuildId;
    String currentBuildId;
    String component;
    ConfigDescriptor configDescriptor;
    SummaryEntry [] entries;
    Variations variations;
    BarGraph bar;
    String outName;
    String title;
    Hashtable scenarioComments;
    
    public FingerPrint() {
    }


    public FingerPrint(String component,ConfigDescriptor config, String reference, String current,Variations variations, String outputDir) {
        this.variations=variations;
        this.component=component;
        referenceBuildId= reference;
        currentBuildId= current;
        outputDirectory= outputDir;
        configDescriptor=config;
        variations.put(PerformanceTestPlugin.BUILD, currentBuildId);
        if (component==null){
        	entries= DB.querySummaries(variations,null);
        	this.component="";
        }
        else
        	entries=DB.querySummaries(variations,component+'%');
       	run(entries);
    }    
    
    /**
     * Creates the fingerprint gif, image map and scenario status table for the component.
     * @param entries - the result of a database query for summaries for a specified variation.
     */
    public void run(Object[] entries) {
        new File(outputDirectory).mkdirs();
        String referenceName=referenceBuildId;
        String currentName=currentBuildId;
        int referenceUnderscoreIndex=referenceBuildId.indexOf('_');
        int currentUnderscoreIndex=currentBuildId.indexOf('_');
        
        if (referenceUnderscoreIndex!=-1)
        	referenceName=referenceBuildId.substring(0,referenceUnderscoreIndex);
        if (currentUnderscoreIndex!=-1)
        	currentName=currentBuildId.substring(0,currentUnderscoreIndex);

        title="Performance of " + component +" "+currentName + " relative to " + referenceName;
        bar= new BarGraph(null);
                
        if (entries != null) {
            for (int i= 0; i < entries.length; i++) {
                SummaryEntry se= (SummaryEntry)entries[i];
                if (se.comment==null)
                	add(bar, se.shortName, new Dim[] { se.dimension }, se.scenarioName);
                else{
                	if (scenarioComments==null)
                		scenarioComments=new Hashtable();
                	scenarioComments.put(se.scenarioName,se.comment);
                	add(bar, se.shortName, new Dim[] { se.dimension }, se.scenarioName,se.comment);
                }
            }
        }
        
        outName= "FP_" + component+ '_'+referenceName + '_' + currentBuildId+"."+configDescriptor.name;
               
        if (component=="")
        	outName= "FP_"+referenceName + '_' + currentBuildId+"."+configDescriptor.name;
        save(bar, outputDirectory + '/' + outName);
        
        //show(bar);
     
       }


    private void add(BarGraph bar, String name, Dim[] dims, String scenarioName) {
    	add (bar,name,dims,scenarioName,null);
    }

    private void add(BarGraph bar, String name, Dim[] dims, String scenarioName,String comment) {
         String refData= "";
        Scenario scenario= DB.getScenarioSeries(scenarioName, variations, PerformanceTestPlugin.BUILD, referenceBuildId, currentBuildId, dims);
        String[] timeSeriesLabels= scenario.getTimeSeriesLabels();
        if (timeSeriesLabels.length == 2) {
            // we mark the label with a '*' or '�' to indicate that no data was available for the specified builds
            if (!timeSeriesLabels[0].equals(referenceBuildId)) {
                name= '*' + name;
                refData= " (" + timeSeriesLabels[0] + ")";
            } else if (!timeSeriesLabels[1].equals(currentBuildId)) {
                name= '�' + name;
                refData= " (" + timeSeriesLabels[1] + ")";
            }
        }
        
        for (int i= 0; i < dims.length; i++) {
            TimeSeries timeSeries= scenario.getTimeSeries(dims[i]);
	        int l= timeSeries.getLength();
	        if (l >= 1) {
	            double percent= 0.0;
	            if (l > 1) {
	                double ref= timeSeries.getValue(0);
	                double val= timeSeries.getValue(1);
	            	percent= 100.0 - ((val / ref) * 100.0);
	            }
	            if (Math.abs(percent) < 200) {
	                String n= name + " (" + dims[i].getName() + ")" + refData;
                	bar.addItem(n, percent,configDescriptor.url+"/"+(scenarioName.replace('#','.').replace(':','_').replace('\\','_'))+".html#"+dims[i].getName(),comment); //$NON-NLS-1$ //$NON-NLS-2$
	            }
	        }
        }
        //scenario.dump(System.out);
	         
    }

    private void save(BarGraph bar, String output) {

    	//if (bar.getFItems().size()==0)
    		//return;
    	
        Display display= Display.getDefault();
        
        int height= bar.getHeight();
        Image image= new Image(display, GRAPH_WIDTH, height);
        
        GC gc= new GC(image);
        bar.paint(display, GRAPH_WIDTH, height, gc);
        gc.dispose();

        ImageData data = Utils.downSample(image);
        ImageLoader il= new ImageLoader();
        il.data= new ImageData[] { data };

        OutputStream out= null;
        try {
            out= new BufferedOutputStream(new FileOutputStream(output + ".gif")); //$NON-NLS-1$
            //System.out.println("writing: " + output);
            il.save(out, SWT.IMAGE_GIF);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            image.dispose();
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e1) {
                    // silently ignored
                }
            }
        }
    }
    
    /*
     * Displays bar graph in window
     */
    private void show(final BarGraph bar) {
        Display display= new Display();
        
        Shell shell= new Shell(display);
        shell.setLayout(new FillLayout());
        
        final Canvas canvas= new Canvas(shell, SWT.NO_BACKGROUND);
        canvas.addPaintListener(new PaintListener() {
            public void paintControl(PaintEvent e) {
                Point s= canvas.getSize();
                bar.paint(canvas.getDisplay(), s.x, s.y, e.gc);
            }
        });
        
        shell.open();

        while (!shell.isDisposed())
            if (!display.readAndDispatch())
                display.sleep();
            
        display.dispose();
    }
	public BarGraph getBar() {
		return bar;
	}
	public String getOutName() {
		return outName;
	}
}
