package com.ib.omb.beans;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.inject.Named;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.PrimeFaces;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;

/**
 * Страница, която служи, за да се зареждат списъци с версии 
 * на базите данни, и да се изпълняват скриптовете в тях.
 * <ol>
 *   <li>Прави се пакет com.id.docu.upgrade (или друг и се задава пътят му в променливата PACKAGE_UPGRADE_NAME)</li>
 *   <li>В пакета се слагат подпапки с имената на различните бази</li>
 *   <li>За всяка база се слагат файловете с ъпдейти в съответната папка</li>
 *   <li>Имената на файловете спазват следния формат:
 *   	<br><strong>changes_<em>dbName</em>_<em>version</em>.sql</strong>,
 *      <br>където <em>dbName</em> е името на базата, а <em>version</em> е версията на файла.
 *      <br>Версията е във формат x.xx или x.xx.x!
 *      <br>Ето няколко валидни имена за пример:
 *      <br>changes_oracle_1.01.sql, changes_oracle_1.14.5.sql, changes_oracle_0.00.1.sql
 *   </li>
 * </ol>
 * 
 * @author n.kanev
 */
@Named
@ViewScoped
public class LoadChangesFiles extends IndexUIbean implements Serializable {

	private static final long serialVersionUID = -8754916275701209599L;
	private static final Logger LOGGER = LoggerFactory.getLogger(LoadChangesFiles.class);
	public static final String PACKAGE_UPGRADE_NAME = "com.ib.omb.upgrade";
	private static final String PATH_TO_FILES = "WEB-INF/classes/" + PACKAGE_UPGRADE_NAME.replace('.', '/');
	
	private String inputVersionMin = "1.01";
	private String inputVersionMax = "1.01";
	private String inputDbVendor;
	
	private TreeNode treeRoot;
	private List<String> dbVendorNames;
	private String fileContents;
	private List<MyFile> foundFiles;
	private List<MyFile> orderedFiles;
	private String log;
	
	@PostConstruct
	public void init() {
		this.dbVendorNames = new ArrayList<>();
		this.foundFiles = new ArrayList<>();
		this.orderedFiles = new ArrayList<>();
		
		buildTree();
	}
	
	/**
	 * Намира имената и показва всички налични файлове в пакета с файлове за ъпгрейд PACKAGE_UPGRADE_NAME
	 */
	private void buildTree() {
		ExternalContext context = JSFUtils.getExternalContext();
		File root = new File(context.getRealPath(PATH_TO_FILES));
		File[] vendors = root.listFiles();
		
		treeRoot = new DefaultTreeNode("root", null);
		
		MyFile packageFile = new MyFile(null, PACKAGE_UPGRADE_NAME, false);
		TreeNode packageNode = new DefaultTreeNode(packageFile, treeRoot);
		packageNode.setType("package");
		
		if(vendors != null) {
			for(File vendor : vendors) {
				this.dbVendorNames.add(vendor.getName());
				MyFile vendorFile = new MyFile(null, vendor.getName(), false);
				TreeNode vendorNode = new DefaultTreeNode(vendorFile, packageNode);
				vendorNode.setType("package");
				
				if(vendor.isDirectory()) {
					File[] files = vendor.listFiles();
					for(File file : files) {
	
						MyFile fileFile = new MyFile(file.getName(), vendor.getName(), true);
						TreeNode fileNode = new DefaultTreeNode(fileFile, vendorNode);
						fileNode.setType("file");
						
					}
				}
			}
		}
	}
	
	/**
	 * Когато се избере файл в дървото, показва съдържанието му отдясно.
	 * @param file
	 */
	public void onFileSelect(MyFile file) {
		this.fileContents = readFile(file);
	}
	
	/**
	 * Натиснат е бутонът 'избор'. Проверява се какво е избрано в полетата, валидира се,
	 * намират се налични файлове и се показват в реда, в който ще се изпълнят според версията.
	 */
	public void actionUpgrade() {
		// форматът на версията е X.XX или X.XX.X, друго не
		// 1.01.1 е валидно, 1.1.1 не е
		String regex = "(\\d)\\.(\\d{2})(\\.\\d)?";
		Pattern pattern = Pattern.compile(regex);
		
		// валидация на полетата с версии
		
		Matcher m = pattern.matcher(this.inputVersionMin.trim());
		if(!m.matches()) {
			JSFUtils.addMessage("loadChangesForm:inputVersionMin", FacesMessage.SEVERITY_ERROR, "Min версията е с неправилен формат!");
			scrollToMessages();
			return;
		}
		
		m = pattern.matcher(this.inputVersionMax.trim());
		if(!m.matches()) {
			JSFUtils.addMessage("loadChangesForm:inputVersionMax", FacesMessage.SEVERITY_ERROR, "Max версията е с неправилен формат!");
			scrollToMessages();
			return;
		}
		
		// намиране на всички файлове, които са за този DBMS
		
		ExternalContext context = JSFUtils.getExternalContext();
		String folder = String.format("%s/%s", PATH_TO_FILES, this.inputDbVendor.toLowerCase());
		Path path = Paths.get(context.getRealPath(folder));
		
		if(Files.exists(path)) {
			StringBuilder logString = new StringBuilder();
			this.foundFiles.clear();
			this.log = "";
			try {
				// файловете се добавят в списъка с файлове за тзи DBMS, още не са филтрирани и сортирани
				
				try(Stream<Path> files = Files.list(path)){
					files.forEach(p -> this.foundFiles.add(new MyFile(p.getFileName().toString(), this.inputDbVendor.toLowerCase(), true)));
				}
				
			} catch (IOException e) {
				logString.append("Cannot read files!");
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при четене на файлове в пакета!");
				e.printStackTrace();
			}
			
			// филтриране и сортиране на файловете
			this.orderedFiles = this.foundFiles.stream()
					.filter(this.predicateFilterVersions)
					.sorted(this.comparatorSortVersions)
					.collect(Collectors.toList());
			
			// изчитане на съдържанието в реда, в който са сортирани
			writeToLog();
		}
	}
	
	/**
	 * Изчита съдържанието на файловете в реда, в който ще се изпълнят, и го изписва на екрана.
	 * За да се види нагледно дали са подредени правилно.
	 */
	private void writeToLog() {
		StringBuilder sb = new StringBuilder();
		for(MyFile file : this.orderedFiles) {
			String contents = readFile(file);
			if(!contents.isEmpty()) {
				sb.append(readFile(file) + "<br>----------<br>");
			}
		}
		this.log = sb.toString();
	}
	
	/**
	 * Изчита файл и връща съдържанието му.
	 * @param file
	 * @return
	 */
	private String readFile(MyFile file) {
		StringBuilder fileContentString = new StringBuilder();
		
		if(file.isFile) {
			ExternalContext context = JSFUtils.getExternalContext();
			String fileToOpen = String.format("%s/%s/%s", PATH_TO_FILES, file.packageName, file.fileName);
			Path path = Paths.get(context.getRealPath(fileToOpen));
			
			try {
				if(Files.exists(path.toRealPath())) {
					try(BufferedReader reader = Files.newBufferedReader(path, Charset.forName("UTF-8"))) {
						String line = null;
						while((line = reader.readLine()) != null)
							fileContentString.append(line + "\n");
					}
				}
				else {
					fileContentString.append("File does not exist??");
				}
			} catch (IOException e) {
				fileContentString.append("Грешка при четене на файл " + fileToOpen + "!");
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при четене на файл " + fileToOpen + "!");
				e.printStackTrace();
			}
		}
		return fileContentString.toString();
	}
	
	/**
	 * Извършва филтрирането дали един файл е в границите на зададените версии в екрана.
	 * Избира файловете които са с версия (inputVersionMin; inputVersionMax]
	 */
	private Predicate<MyFile> predicateFilterVersions = (file) -> {
		String fileVersion = file.getFileName().split("_")[2];
		fileVersion = fileVersion.substring(0, fileVersion.lastIndexOf('.'));
		fileVersion = fileVersion.replace(".", "");
		if(fileVersion.length() == 3) fileVersion += "0";
		
		String versionMin = this.inputVersionMin.replace(".", "");
		if(versionMin.length() == 3) versionMin += "0";
		
		String versionMax = this.inputVersionMax.replace(".", "");
		if(versionMax.length() == 3) versionMax += "0";
		
		// ако версията на файла е по-голяма от минималната и по-малка или равна на максиманлата избрана
		return (fileVersion.compareTo(versionMin) >= 0 && fileVersion.compareTo(versionMax) <= 0);
	};
	
	/**
	 * Извършва сортиране на файловете по версия
	 */
	private Comparator<MyFile> comparatorSortVersions = (file1, file2) -> {
		String file1Version = file1.getFileName().split("_")[2];
		file1Version = file1Version.substring(0, file1Version.lastIndexOf('.'));
		file1Version = file1Version.replace(".", "");
		if(file1Version.length() == 3) file1Version += "0";
		
		String file2Version = file2.getFileName().split("_")[2];
		file2Version = file2Version.substring(0, file2Version.lastIndexOf('.'));
		file2Version = file2Version.replace(".", "");
		if(file2Version.length() == 3) file2Version += "0";
		
		return file1Version.compareTo(file2Version);
	};
	
	/**
	 * Скролва нагоре екрана, за да се видят съобщенията за грешка.
	 */
	private void scrollToMessages() {
		PrimeFaces.current().executeScript("scrollToErrors()");
	}
	
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	
	public class MyFile {
		private String fileName;
		private String packageName;
		private boolean isFile;
		
		public MyFile(String fileName, String packageName, boolean isFile) {
			super();
			this.fileName = fileName;
			this.packageName = packageName;
			this.isFile = isFile;
		}

		public String getFileName() {
			return fileName;
		}

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		public String getPackageName() {
			return packageName;
		}

		public void setPackageName(String packageName) {
			this.packageName = packageName;
		}

		public boolean isFile() {
			return isFile;
		}

		public void setFile(boolean isFile) {
			this.isFile = isFile;
		}
		
	}
	
	public String getInputVersionMin() {
		return inputVersionMin;
	}

	public void setInputVersionMin(String inputVersionMin) {
		this.inputVersionMin = inputVersionMin;
	}

	public String getInputVersionMax() {
		return inputVersionMax;
	}

	public void setInputVersionMax(String inputVersionMax) {
		this.inputVersionMax = inputVersionMax;
	}

	public String getInputDbVendor() {
		return inputDbVendor;
	}

	public void setInputDbVendor(String inputDbVendor) {
		this.inputDbVendor = inputDbVendor;
	}

	public String getFileContents() {
		return fileContents;
	}

	public void setFileContents(String fileContents) {
		this.fileContents = fileContents;
	}

	public TreeNode getTreeRoot() {
		return treeRoot;
	}

	public void setTreeRoot(TreeNode treeRoot) {
		this.treeRoot = treeRoot;
	}

	public List<String> getDbVendorNames() {
		return this.dbVendorNames;
	}

	public List<MyFile> getFoundFiles() {
		return foundFiles;
	}

	public void setFoundFiles(List<MyFile> foundFiles) {
		this.foundFiles = foundFiles;
	}
	

	public List<MyFile> getOrderedFiles() {
		return orderedFiles;
	}

	public void setOrderedFiles(List<MyFile> orderedFiles) {
		this.orderedFiles = orderedFiles;
	}

	public String getLog() {
		return log;
	}

	public void setLog(String log) {
		this.log = log;
	}
	
}
