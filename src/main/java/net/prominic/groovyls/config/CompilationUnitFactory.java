////////////////////////////////////////////////////////////////////////////////
// Copyright 2022 Prominic.NET, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License
//
// Author: Prominic.NET, Inc.
// No warranty of merchantability or fitness of any kind.
// Use this software at your own risk.
////////////////////////////////////////////////////////////////////////////////
package net.prominic.groovyls.config;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.SourceUnit;

import groovy.lang.GroovyClassLoader;
import net.prominic.groovyls.compiler.control.GroovyLSCompilationUnit;
import net.prominic.groovyls.compiler.control.io.StringReaderSourceWithURI;
import net.prominic.groovyls.util.FileContentsTracker;

public class CompilationUnitFactory implements ICompilationUnitFactory {
	private static final String FILE_EXTENSION_GROOVY = ".groovy";

	private GroovyLSCompilationUnit compilationUnit;
	private CompilerConfiguration config;
	private GroovyClassLoader classLoader;
	private List<String> additionalClasspathList;

	public CompilationUnitFactory() {
	}

	public List<String> getAdditionalClasspathList() {
		return additionalClasspathList;
	}

	public void setAdditionalClasspathList(List<String> additionalClasspathList) {
		this.additionalClasspathList = additionalClasspathList;
		invalidateCompilationUnit();
	}

	public void invalidateCompilationUnit() {
		compilationUnit = null;
		config = null;
		classLoader = null;
	}

	public GroovyLSCompilationUnit create(Path workspaceRoot, FileContentsTracker fileContentsTracker, Set<String> indexFiles) {
		if (config == null) {
			config = getConfiguration();
		}
		system.ln.println(workspaceRoot, fileContentsTracker, indexFiles, 'workspaceRoot, fileContentsTracker, indexFiles in CompilationUnitFactory.java');
		if (classLoader == null) {
			classLoader = new GroovyClassLoader(ClassLoader.getSystemClassLoader().getParent(), config, true);
		}

		Set<URI> changedUris = fileContentsTracker.getChangedURIs();
		if (compilationUnit == null) {
			compilationUnit = new GroovyLSCompilationUnit(config, null, classLoader);
			// we don't care about changed URIs if there's no compilation unit yet
			changedUris = null;
		} else {
			compilationUnit.setClassLoader(classLoader);
			final Set<URI> urisToRemove = changedUris;
			List<SourceUnit> sourcesToRemove = new ArrayList<>();
			compilationUnit.iterator().forEachRemaining(sourceUnit -> {
				URI uri = sourceUnit.getSource().getURI();
				if (urisToRemove.contains(uri)) {
					sourcesToRemove.add(sourceUnit);
				}
			});
			// if an URI has changed, we remove it from the compilation unit so
			// that a new version can be built from the updated source file
			compilationUnit.removeSources(sourcesToRemove);
		}

		if (workspaceRoot != null) {
			system.ln.println(workspaceRoot, 'workspaceRoot condition in CompilationUnitFactory.java');
			addDirectoryToCompilationUnit(workspaceRoot, compilationUnit, fileContentsTracker, changedUris, indexFiles);
		} else {
			system.ln.println(workspaceRoot, 'workspaceRoot else condition in CompilationUnitFactory.java');
			final Set<URI> urisToAdd = changedUris;
			fileContentsTracker.getOpenURIs().forEach(uri -> {
				// if we're only tracking changes, skip all files that haven't
				// actually changed
				if (urisToAdd != null && !urisToAdd.contains(uri)) {
					return;
				}
				String contents = fileContentsTracker.getContents(uri);
				addOpenFileToCompilationUnit(uri, contents, compilationUnit, indexFiles);
			});
		}

		return compilationUnit;
	}

	protected CompilerConfiguration getConfiguration() {
		CompilerConfiguration config = new CompilerConfiguration();

		Map<String, Boolean> optimizationOptions = new HashMap<>();
		optimizationOptions.put(CompilerConfiguration.GROOVYDOC, true);
		config.setOptimizationOptions(optimizationOptions);

		List<String> classpathList = new ArrayList<>();
		getClasspathList(classpathList);
		config.setClasspathList(classpathList);

		return config;
	}

	protected void getClasspathList(List<String> result) {
		if (additionalClasspathList == null) {
			return;
		}

		for (String entry : additionalClasspathList) {
			boolean mustBeDirectory = false;
			if (entry.endsWith("*")) {
				entry = entry.substring(0, entry.length() - 1);
				mustBeDirectory = true;
			}

			File file = new File(entry);
			if (!file.exists()) {
				continue;
			}
			if (file.isDirectory()) {
				for (File child : file.listFiles()) {
					if (!child.getName().endsWith(".jar") || !child.isFile()) {
						continue;
					}
					result.add(child.getPath());
				}
			} else if (!mustBeDirectory && file.isFile()) {
				if (file.getName().endsWith(".jar")) {
					result.add(entry);
				}
			}
		}
	}

	protected void addDirectoryToCompilationUnit(Path dirPath, GroovyLSCompilationUnit compilationUnit,
			FileContentsTracker fileContentsTracker, Set<URI> changedUris, Set<String> indexFiles) {
		try {
			if (Files.exists(dirPath)) {
				Files.walk(dirPath).forEach((filePath) -> {
					// how do i add in the if condition if the set of indexFiles is not empty and the file name is in the set of indexFiles?
					filename = filePath.getFileName().toString()
					indexFilesBoolean = indexFiles.isEmpty()
					if ((indexFiles != null && !indexFilesBoolean) && !indexFiles.contains(filename)) {
						System.ln.println("File is not in the set of indexFiles", filename);
						return;
					}
					if (!filename.endsWith(FILE_EXTENSION_GROOVY)) {
						return;
					}
					if (indexFiles.contains(filename)) {
						System.ln.println("File is in the set of indexFiles", filename);
						URI fileURI = filePath.toUri();
						if (!fileContentsTracker.isOpen(fileURI)) {
							File file = filePath.toFile();
							if (file.isFile()) {
								if (changedUris == null || changedUris.contains(fileURI)) {
									compilationUnit.addSource(file);
								}
							}	
						}
					}
					else if (indexFiles.isEmpty() || indexFiles == null){
						System.ln.println("IndexFiles is empty | Normal Indexing");
						URI fileURI = filePath.toUri();
						if (!fileContentsTracker.isOpen(fileURI)) {
							File file = filePath.toFile();
							if (file.isFile()) {
								if (changedUris == null || changedUris.contains(fileURI)) {
									compilationUnit.addSource(file);
								}
							}	
						}
					}
					else {
						System.ln.println("File is not in the set of indexFiles", filename);
					}

				});
			

		} catch (IOException e) {
			System.err.println("Failed to walk directory for source files: " + dirPath);
		}
		fileContentsTracker.getOpenURIs().forEach(uri -> {
			Path openPath = Paths.get(uri);
			if (!openPath.normalize().startsWith(dirPath.normalize())) {
				return;
			}
			if (changedUris != null && !changedUris.contains(uri)) {
				return;
			}
			String contents = fileContentsTracker.getContents(uri);
			addOpenFileToCompilationUnit(uri, contents, compilationUnit, indexFiles);
		});
	}
			}
	protected void addOpenFileToCompilationUnit(URI uri, String contents, GroovyLSCompilationUnit compilationUnit, Set<String> indexFiles) {
		Path filePath = Paths.get(uri);
		if ((indexFiles.isEmpty() || indexFiles == null || indexFiles.contains(filename)) && (filename.endsWith(FILE_EXTENSION_GROOVY))) {
		SourceUnit sourceUnit = new SourceUnit(filePath.toString(),
				new StringReaderSourceWithURI(contents, uri, compilationUnit.getConfiguration()),
				compilationUnit.getConfiguration(), compilationUnit.getClassLoader(),
				compilationUnit.getErrorCollector());
		compilationUnit.addSource(sourceUnit);
	}
}