package com.g2m.services.strategybuilder.utilities;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SecurityFilePathCreator {

	public SecurityFilePathCreator() {

	}

	public static List<File> getFilesFromPaths(List<String> paths){
		
		List<File> files = new ArrayList<File>();
		
		for (String path : paths){
			if (path.contains("/"))
				files.add(getFileFromRemoteDirectory(path));
			else 
				files.add(getFileFromLocalResourcDirectory(path));
		}
		
		return files;
	}

	private static File getFileFromLocalResourcDirectory(String path){
		URL url = ClassLoader.getSystemResource(path);
		File file;
		try {
			file = new File(url.toURI());
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
		return file;
	}

	private static File getFileFromRemoteDirectory(String path){
		File file = null;
		try {
			URL url = new File(path).toURI().toURL();
			file = new File(url.toURI());
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		return file;
	}


}
