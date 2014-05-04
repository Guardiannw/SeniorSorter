/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.moorestudio.seniorimageprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import static java.lang.Character.isDigit;
import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import static java.nio.file.Files.copy;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.sort;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.apache.commons.imaging.ImageReadException;
import static org.apache.commons.imaging.Imaging.getMetadata;
import org.apache.commons.imaging.common.IImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;

/**
 *
 * @author nrwebb
 */
public class SeniorSorter implements Runnable
{
    private UserInterface parent;
    private File cameraDirectory;
    private File imageDirectory;
    private File dataFile;
    private HashMap<File, Long> imageData;
    private HashMap<String, Long> timestampData; // An hashMap of a students id and the timestamp that it was scanned
    private int id;
    
    public SeniorSorter(File directory, UserInterface prnt, int threadId) throws IOException
    {
        try
        {
            id = threadId;
            parent = prnt;
            cameraDirectory = directory;
            imageData = new HashMap<>();
            timestampData = new HashMap<>();

            // set the image directory
            imageDirectory = new File(cameraDirectory.getAbsolutePath() + "/Images");

            // set the data file
            dataFile = new File(cameraDirectory.getAbsolutePath() + "/Data").listFiles()[0]; // get the first file out of the data directory, needs to have ONLY 1 FILE!!!

            String errorMessage = "";
            if(!imageDirectory.exists())
            {
                errorMessage += "Image directory: " + imageDirectory.getAbsolutePath() + " ";
            }
            if(!dataFile.exists())
            {
                errorMessage += "Data file: " + dataFile.getAbsolutePath() + " ";
            }
            if(!imageDirectory.exists() || !dataFile.exists())
            {
                errorMessage += "do(es) not exist!";
                throw new IOException(errorMessage);
            }
        }
        catch(NullPointerException e)
        {
            throw new IOException("Missing data file under the directory: " + cameraDirectory.getAbsolutePath() + "/Data/");
        }
    }
    
    //get all of the image timestamps
    public void getTimestampData() throws ImageReadException, IOException, ParseException
    {
        int numImages = imageData.size();
        for (File image : imageData.keySet()) {
            IImageMetadata metadata = getMetadata(image);
            if(metadata instanceof JpegImageMetadata)
            {
                JpegImageMetadata md = (JpegImageMetadata) metadata;
                String dateTime = md.findEXIFValue(TiffTagConstants.TIFF_TAG_DATE_TIME).getStringValue();
                
                //if the dateTime is null, then throw an error
                if(dateTime == null || dateTime.isEmpty())
                {
                    throw new ImageReadException("Images do not contain the EXIF date information.");
                }
                else
                {
                    //convert from EXIF format to a date
                    SimpleDateFormat dateParser = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
                    imageData.put(image, dateParser.parse(dateTime).getTime());
                }
                
                //update the gui
                parent.addProgress((.125 / parent.numThreads) / numImages);
            }
            else
            {
                throw new ImageReadException("Images do not contain the EXIF date information.");
            }
        }
    }
    
    //TODO: get the line count for the updating of the GUI.
    public int getLineCount(File file) throws FileNotFoundException, IOException
    {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        
        int count = 0; 
        while(reader.readLine() != null)
        {
            count++;
        }
        
        return count;
    }
    
    public void getStudentData() throws FileNotFoundException, Exception
    {
        try
        {
            Scanner scanner = new Scanner(dataFile);
            
            //get the line count for the GUI
            int lineCount = getLineCount(dataFile);
            
            //the csv file does not contain headers
            while(scanner.hasNextLine())
            {
                    //Create the new student map
                    String studentId;
                    Long timestamp;
                    String line = scanner.nextLine();
                    //check to make sure there arent just empty rows
                    if(!line.replace("\"", "").replace(",", "").isEmpty())
                    {
                        String[] lineInformation = line.replace("\"", "").split(",");
                        SimpleDateFormat dateParser = new SimpleDateFormat("M/dd/yy HH:mm:ss");
                        studentId = lineInformation[0];
                        timestamp = dateParser.parse(lineInformation[3] + " " + lineInformation[2]).getTime();
                        timestampData.put(studentId, timestamp);
                    }
                    
                    //update the GUI
                    parent.addProgress((.125 / parent.numThreads) / lineCount);
            }
        }
        catch(FileNotFoundException e)
        {
            throw new FileNotFoundException("Data file at: " + dataFile.getAbsolutePath() + " does not exist!");
        }
        catch (ParseException ex) {
            throw new Exception("The date in the data file at: " + dataFile.getAbsolutePath() + " is not formatted correctly!");
        }
    }
    
    public void getImageFiles()
    {
        FilenameFilter imageFilter = (File dir, String name) -> {
            String extension = name.substring(name.lastIndexOf('.')).toLowerCase();
            List<String> acceptableExtensions = asList(parent.imageTypes);
            return acceptableExtensions.contains(extension);
        };
        
        File[] imageFiles = imageDirectory.listFiles(imageFilter);
        int imageCount = imageFiles.length;
        for(File image : imageFiles)
        {
            imageData.put(image, (long)0);
            
            //update the GUI
            parent.addProgress((.125 / parent.numThreads) / imageCount);
            
        }
    }
    
    //sort the images with the timestamps
    public void sortImages()
    {
        LinkedList<Map.Entry<String, Long>> timestampList = new LinkedList<>(timestampData.entrySet());
        sort(timestampList, (x, y) -> x.getValue() > y.getValue() ? -1 : x.getValue().equals(y.getValue()) ? 0 : 1);
        // Sort in reverse so that the most recent timestamps are first.e so that the most recent timestamps are first.
        
        LinkedList<Map.Entry<File, Long>> imageDataList = new LinkedList<>(imageData.entrySet());
        sort(imageDataList, (x, y) -> x.getValue() > y.getValue() ? -1 : x.getValue().equals(y.getValue()) ? 0 : 1);  // Sort in reverse so that the most recent timestamps are first.
        
        // For the gui update
        int idCount = imageDataList.size();
        
        //add the file to the top timestamp student until it is no longer more than it
        while(!timestampList.isEmpty() && !imageDataList.isEmpty())
        {
            Map.Entry<File, Long> iData = imageDataList.peekFirst();
            Map.Entry<String, Long> tsData = timestampList.pollFirst();
            ArrayList<File> studentImages = new ArrayList<>();
            while(!imageDataList.isEmpty() && iData.getValue() > tsData.getValue())
            {
                iData = imageDataList.pollFirst();
                studentImages.add(iData.getKey());
                iData = imageDataList.peekFirst();
                //update the GUI
                parent.addProgress((.125 / parent.numThreads) / idCount);
            }
            if(!studentImages.isEmpty())
            {
                parent.addImagesToStudent(tsData.getKey(), studentImages);
            }
        }
        
        //add the unsorted images to the parent's unsorted queue
        for(Map.Entry<File, Long> entry : imageDataList)
        {
            parent.unsortedFiles.add(entry.getKey());
            //update the GUI
            parent.addProgress((.125 / parent.numThreads) / idCount);
        }
    }
    
    
    private File makeStudentFolder(HashMap<String, String> studentInfo)
    {
        //Get the student last name and first name and School name
        String firstName = studentInfo.get("FIRST");
        String lastName = studentInfo.get("LAST");
        String schoolName = studentInfo.get("SCHOOL");
        
        String folderName = lastName.toUpperCase() + "_" + firstName.toUpperCase();
        
        //see if the folder exists, if it doesn't then make it.
        File studentFolder = new File(parent.studentSchoolDirectories.get(schoolName), folderName);
        if(!studentFolder.exists())
        {
            studentFolder.mkdir();
        }
        
        return studentFolder;
    }
    
    
    private void saveImages(HashMap<String, String> studentInfo, File activeStudentFolder, ArrayList<File> studentImages) throws IOException
    {
        // Get the student information
        String schoolName = studentInfo.get("SCHOOL").replaceAll(" ", "_");  // If the filenames have spaces then an error will be thrown!
        String year = studentInfo.get("YEAR").replaceAll(" ", "");
        String lastName = studentInfo.get("LAST").replaceAll(" ", "");
        String firstName = studentInfo.get("FIRST").replaceAll(" ", "");
        
        //Write out the images
        for(File image : studentImages)
        {
            String oldImageName = image.getName();
            //Get the image extension
            String ext = oldImageName.substring(oldImageName.lastIndexOf("."));
            
            //Make the index string
            StringBuilder indexString = new StringBuilder(valueOf(parent.pollNextImageIndex()));
            
            //If image index is less than the desired number of digits, then padd it.
            while(indexString.length() < parent.getNumImageIndexDigits())
            {
                indexString.insert(0, '0');
            }
            //Make the new file
            File newFile = new File(activeStudentFolder, schoolName + year + lastName + indexString.toString() + ext);
            
            //Copy and rename the old file to the new file
            copy(image.toPath(), newFile.toPath());
            parent.addProgress(((.5 / parent.studentImages.size()) / studentImages.size()));
        }
        
    }

    @Override
    public void run() {
        
        try 
        {
            getImageFiles();
            
            getTimestampData();

            getStudentData();
            
            sortImages();
            
            //tell the parent thread that we are finished and ready to write
            parent.acknowledgeReadyToWrite();
            
            String activeStudentId = null;
            while((activeStudentId = parent.availableStudents.poll()) != null)
            {
                HashMap<String, String> studentInfo = parent.studentInformation.get(activeStudentId);
                File studentFolder = makeStudentFolder(studentInfo);
                saveImages(studentInfo, studentFolder, parent.studentImages.get(activeStudentId));
            }
            
            parent.acknowledgeFinished();
        }
        catch(Exception e)
        {
            parent.throwException(e);
        }
        
    }
}
