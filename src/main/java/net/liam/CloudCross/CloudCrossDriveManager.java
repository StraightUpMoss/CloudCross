package net.liam.CloudCross;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;

import java.net.http.HttpRequest;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
/* class to demonstrate use of Drive files list API */
public class CloudCrossDriveManager {

  private static final String APPLICATION_NAME = "CloudCross";

  private static final String TOKENS_DIRECTORY_PATH = "tokens";

  private static final List<String> SCOPES =
          Collections.singletonList(DriveScopes.DRIVE_FILE);
  private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
  private static final String CONFIG_FILE_PATH = "/CloudCrossConfig.json";
  private static final String SavePrefix = "CROSSCLOUDSAVE_";
  //private static String SaveName = "World";
  private static final String[] directory = new String[]{"CLOUDCROSS", "SAVES"};
  private static final List<String> fileNames = new ArrayList<String>();
  private static final List<String> fileIds = new ArrayList<String>();
  private static final List<java.io.File> filesToUpload = new ArrayList<java.io.File>();
  private static final List<String> worldNames = new ArrayList<>();
  private static int fileBackupLimit = 5;
  private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  /**
   * Creates an authorized Credential object.
   *
   * @param HTTP_TRANSPORT The network HTTP Transport.
   * @return An authorized Credential object.
   * @throws IOException If the credentials.json file cannot be found.
   */
  private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
          throws IOException {
    // Load client secrets.
    InputStream in = CloudCrossDriveManager.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
    if (in == null) {
      throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
    }
    GoogleClientSecrets clientSecrets =
            GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

    // Build flow and trigger user authorization request.
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
            .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build();
    LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
    Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    //returns an authorized Credential object.
    return credential;
  }
  public static void main(String... args) throws IOException, GeneralSecurityException {
    // Build a new authorized API client service.
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
            .setApplicationName(APPLICATION_NAME)
            .build();
    System.out.println("Made it here");
    GetUploadedFiles(service);
    GatherConfigData();
    if(fileNames.isEmpty()) {
      OrganizeDirectory(service, directory);
      System.out.println(fileNames);
    }
    else {
      System.out.println(fileNames);
    }
    //UploadSubscribedFiles(service);
    //DownloadSubscribedFiles(service);

  }
  //Update to use the resumable upload maybe?
  //Add something that limits the amount of backup files/ same files
  private static void UploadSubscribedFiles(Drive service) throws IOException{
    for (int i = 0; i < filesToUpload.size(); i++) {
      java.io.File f = filesToUpload.get(i);
      String worldName = worldNames.get(i);
      ZipFile(f.getAbsolutePath(),f.getAbsolutePath() + ".zip");
      UploadFile(service, f.getAbsolutePath() + ".zip", worldName, "SAVES");
    }
  }
  private static void DownloadSubscribedFiles(Drive service) throws IOException {
    for (int i = 0; i < filesToUpload.size(); i++) {
      java.io.File f = filesToUpload.get(i);
      String worldName = worldNames.get(i);
      DownloadFile(service, GetMostRecentBackupId(worldName), f.getAbsolutePath() +".zip", true);
    }
  }
  public static void UploadFileFromMC(String absolutePath) throws IOException, GeneralSecurityException {
    try {
      for (int i = 0; i < filesToUpload.size(); i++) {
        java.io.File file = filesToUpload.get(i);
        if (file.getAbsolutePath() == absolutePath) {
          final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
          Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                  .setApplicationName(APPLICATION_NAME)
                  .build();
          UploadFile(service, absolutePath, worldNames.get(i), "SAVES");
          return;
        }
      }
      System.out.println("File not configured to upload");
    }
    catch(IOException e) {
      System.err.println("Failed to upload from mc: " + e);
    }
    catch (GeneralSecurityException e) {
      System.err.println("Security error: " + e);
    }
  }
  private static void UploadFile(Drive service, String absolutePath, String SaveName, String destinationFolderName) throws IOException {
    int backupCount = 0;
    for(String s : fileNames) {
      if(s.length() > SavePrefix.length() && s.substring(SavePrefix.length(), s.length()-2).equals(SaveName)) backupCount ++;
      //System.out.println("Backup Count: " + backupCount);
    }
    //override old save with same name
    if (backupCount >= fileBackupLimit) {
      System.out.println("Replacing old backup");
      String oldFileId = FindFileIdFromName(SavePrefix+SaveName+"_0");
      service.files().delete(oldFileId).execute();
      fileNames.remove(SavePrefix + SaveName + "_0");
      //System.out.println(fileNames);
      fileIds.remove(oldFileId);
      for(int s = 0; s < fileNames.size(); s ++) {
        if(fileNames.get(s).length() > SavePrefix.length() && fileNames.get(s).substring(SavePrefix.length(),fileNames.get(s).length()-2).equals(SaveName)) {
          String prevName = fileNames.get(s);
          fileNames.set(s, prevName.substring(0, prevName.length()-2) + "_" + (Integer.parseInt(prevName.substring(prevName.length()-1)) -1));
          com.google.api.services.drive.model.File file = new com.google.api.services.drive.model.File();
          file.setName(fileNames.get(s));
          service.files().update(fileIds.get(s), file).execute();
          //System.out.println(fileNames.get(s));
        }
      }
      UploadFile(service, absolutePath, SaveName, destinationFolderName);
    }
    //create new backup or fresh file
    else {
      System.out.println("Uploading new backup/ save");
      com.google.api.services.drive.model.File fileMetaData = new com.google.api.services.drive.model.File();
      fileMetaData.setName(SavePrefix + SaveName + "_" + backupCount);
      //java.io.File filePath = new java.io.File(ZIP_FILE_PATH);
      java.io.File filePath = new java.io.File(absolutePath);
      FileContent mediaContent = new FileContent("application/zip", filePath);
      fileMetaData.setParents(Collections.singletonList(FindFileIdFromName(destinationFolderName)));
      try {
        com.google.api.services.drive.model.File file  = service.files().create(fileMetaData, mediaContent).setFields("name, id, parents").execute();
        AddFileToDictionary(file);
        //System.out.println("File ID: " + file.getId());
        //return file.getId();
      }
      catch(GoogleJsonResponseException e) {
        System.err.println("Unable to upload file: " + e.getDetails());
        throw e;
      }
      System.out.println("File Uploaded");
    }
  }
  private static void DownloadFile(Drive service, String fileId, String outputDirectory) throws IOException {
    try {
      OutputStream writeStream = new FileOutputStream(outputDirectory);
      service.files().get(fileId)
              .executeMediaAndDownloadTo(writeStream);
      writeStream.flush();
      writeStream.close();
      System.out.println("File Downloaded!");

    }
    catch(GoogleJsonResponseException e) {
      System.err.println("Unable to download file: " + e.getDetails());
      throw e;
    }
  }
  private static void DownloadFile(Drive service, String fileId, String outputDirectory, boolean unzip) throws IOException {
    try {
      OutputStream writeStream = new FileOutputStream(outputDirectory);
      service.files().get(fileId)
              .executeMediaAndDownloadTo(writeStream);
      writeStream.flush();
      writeStream.close();
      System.out.println("File Downloaded!");
      if(unzip) UnzipFile(outputDirectory, outputDirectory.substring(0, outputDirectory.length()-4));
    }
    catch(GoogleJsonResponseException e) {
      System.err.println("Unable to download file: " + e.getDetails());
      throw e;
    }

  }
  private static void ZipFile(String source, String destination) throws IOException {
    java.io.File sourceDirectory = new java.io.File(source);
    FileOutputStream fileOutputStream = new FileOutputStream(destination);
    ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
    Files.walkFileTree(sourceDirectory.toPath(), new SimpleFileVisitor<Path>() {
      public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(sourceDirectory.toPath().relativize(file).toString()));
        Files.copy(file, zipOutputStream);
        return FileVisitResult.CONTINUE;
      }
    });
    zipOutputStream.flush();
    zipOutputStream.close();
    fileOutputStream.close();
    System.out.println("File zipped");
  }
  private static void UnzipFile(String source, String destination) throws IOException {
    byte[] buffer = new byte[1024];
    java.io.File destinationFolder = new java.io.File(destination);
    ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(source));
    ZipEntry zipEntry = zipInputStream.getNextEntry();
    while (zipEntry != null) {
      java.io.File newFile = newFile(destinationFolder,zipEntry);
      if (zipEntry.isDirectory()) {
        if (newFile.isDirectory() && !newFile.mkdirs()) {
          throw new IOException("Failed to create directory: " + newFile);
        }
      }
      else {
        java.io.File parent = newFile.getParentFile();
        if(!parent.isDirectory() && !parent.mkdirs()) {
          throw new IOException("Failed to create directory: " + parent);
        }
        FileOutputStream fileOutputStream = new FileOutputStream(newFile);
        int length;
        while ((length = zipInputStream.read(buffer)) > 0) {
          fileOutputStream.write(buffer, 0, length);
        }
        fileOutputStream.flush();
        fileOutputStream.close();
      }
      zipEntry = zipInputStream.getNextEntry();
    }
    zipInputStream.close();
    System.out.println("File unzipped");
  }
  private static List<com.google.api.services.drive.model.File> GetUploadedFiles(Drive service) throws IOException {
    FileList result = service.files().list()
            .setPageSize(25)
            .setFields("nextPageToken, files(id, name)")
            .execute();
    List<com.google.api.services.drive.model.File> files = result.getFiles();
    if (files == null || files.isEmpty()) {
      System.out.println("No files found.");
    } else {
      //System.out.println("Files:");
      for (com.google.api.services.drive.model.File file : files) {
        AddFileToDictionary(file);
        //System.out.printf("%s (%s)\n", file.getName(), file.getId());
      }
    }
    return files;
  }
  private static java.io.File newFile(java.io.File destination, ZipEntry zipEntry) throws IOException {
    java.io.File destinationFile = new java.io.File(destination, zipEntry.getName());
    String destinationDirPath = destination.getCanonicalPath();
    String destinationFilePath = destinationFile.getCanonicalPath();
    if(!destinationFilePath.startsWith(destinationDirPath + java.io.File.separator)) {
      throw new IOException("Entry is outside of target directory: " + zipEntry.getName());
    }
    return destinationFile;
  }
  private static void OrganizeDirectory(Drive service, String[] directoryPath) throws IOException {
    String parentDirectory = CreateFolder(service, directoryPath[0], "");
    String finalPath = null;
    for(int i = 1; i < directoryPath.length; i++) {
      //System.out.println(parentDirectory);
      finalPath = CreateFolder(service, directoryPath[i], parentDirectory);
    }
   // return finalPath;
  }
  private static String CreateFolder(Drive service, String folderName, String parentFolderId) throws IOException {
    com.google.api.services.drive.model.File fileMetaData = new com.google.api.services.drive.model.File();
    fileMetaData.setName(folderName);
    fileMetaData.setMimeType("application/vnd.google-apps.folder");

    if(!parentFolderId.isEmpty()) {
      try {
        fileMetaData.setParents(Collections.singletonList(parentFolderId));
        com.google.api.services.drive.model.File file = service.files().create(fileMetaData).setFields("name, id, parents").execute();
        System.out.println("Folder ID: " + file.getId() + "\nParent Folder: " + parentFolderId);
        AddFileToDictionary(file);
        return file.getId();
      }
      catch (GoogleJsonResponseException e) {
        System.err.println("Unable to create folder: " + e.getDetails());
        throw e;
      }
    }
    else {
      try {
        com.google.api.services.drive.model.File file = service.files().create(fileMetaData).setFields("name, id").execute();
        System.out.println("Folder ID: " + file.getId());
        AddFileToDictionary(file);
        return file.getId();
      }
      catch (GoogleJsonResponseException e) {
        System.err.println("Unable to create folder: " + e.getDetails());
        throw e;
      }
    }

  }
  private static void AddFileToUploadList(String filePath, String worldName) {
    filesToUpload.add(new java.io.File(filePath));
    worldNames.add(worldName);
  }
  private static String FindFileIdFromName(String name) {
    for (int i = 0; i < fileNames.size(); i ++) {
      if (name.equals(fileNames.get(i))) return fileIds.get(i);
    }
    System.err.println("File with name: " + name + ", not found!");
    return null;
  }
  private static void AddFileToDictionary(com.google.api.services.drive.model.File file) {
    fileNames.add(file.getName());
    fileIds.add(file.getId());
  }
  private static String GetMostRecentBackupId(String name) {
    int latestBackup = 0;
    for(String s : fileNames) {
      if(s.length() > SavePrefix.length() && s.substring(SavePrefix.length(), s.length()-2).equals(name)) {
        if(Integer.parseInt(s.substring(s.length()-1)) > latestBackup) latestBackup = Integer.parseInt(s.substring(s.length()-1));
      }
    }

    return FindFileIdFromName(SavePrefix + name + "_"+latestBackup);
  }
  private static void GatherConfigData() throws IOException {
    //Find a way to read world data from config.json and find a way to make paths relative to this file
    InputStream iS = CloudCrossDriveManager.class.getResourceAsStream(CONFIG_FILE_PATH);
    InputStreamReader fileReader = new InputStreamReader(iS, StandardCharsets.UTF_8);
    JsonObject obj = (JsonObject) new JsonParser().parse(fileReader);
    JsonObject config = (JsonObject) obj.get("Config");
    fileBackupLimit = config.get("BackupsPerWorld").getAsInt();
    JsonArray worldArr = (JsonArray) config.get("Worlds");
    for(Object j: worldArr) {
      JsonObject jObj = (JsonObject) j;
      String path = jObj.get("FilePath").getAsString();
      String worldName = jObj.get("SaveName").getAsString();
      if (jObj.get("Enabled").getAsBoolean()) AddFileToUploadList(path, worldName);
      else System.out.println("Save disabled for world with path: " + worldName);
    }

    //System.out.println(worldArr);
  }
}