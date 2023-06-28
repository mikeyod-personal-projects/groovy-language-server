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
package net.prominic.groovyls;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SignatureHelpOptions;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

import net.prominic.groovyls.config.CompilationUnitFactory;
import net.prominic.groovyls.config.ICompilationUnitFactory;

public class GroovyLanguageServer implements LanguageServer, LanguageClientAware {

    public static void main(String[] args) {
        System.out.print("HELP1");
        InputStream systemIn = System.in;
        OutputStream systemOut = System.out;
        // redirect System.out to System.err because we need to prevent
        // System.out from receiving anything that isn't an LSP message
        System.setOut(new PrintStream(System.err));
        System.out.println("systemIn: " + systemIn);
        System.out.println("systemOut: " + systemOut);
        GroovyLanguageServer server = new GroovyLanguageServer();
        Launcher<LanguageClient> launcher = Launcher.createLauncher(server, LanguageClient.class, systemIn, systemOut);
        server.connect(launcher.getRemoteProxy());
        launcher.startListening();
    }

    private GroovyServices groovyServices;

    public GroovyLanguageServer() {
        this(new CompilationUnitFactory());
    }

    public GroovyLanguageServer(ICompilationUnitFactory compilationUnitFactory) {
        this.groovyServices = new GroovyServices(compilationUnitFactory);
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        String rootUriString = params.getRootUri();
        // System.out.print("In initialize HERE");
        Object paramsObject = params.getInitializationOptions();
        // System.out.println("In initialize HERE3\n");
        Gson gson = new Gson();
        String jsonString = gson.toJson(paramsObject);
        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> myMap = gson.fromJson(jsonString, type);
        // Object indexFilesValue = myMap.get("indexFiles");
        List<Object> indexFilesListObject = (List<Object>) myMap.get("indexFiles");
        System.out.println("print indexfileslist objects \n");
        for (Object item : indexFilesListObject) {
            System.out.println(item.getClass());
        }
        // System.out.println("print indexfile list strings \n");
        List<String> indexFilesList = (List<String>) myMap.get("indexFiles");
        // System.out.println(indexFilesList);
        System.out.println("print hashset strings \n");
        Set<String> fileSet = new HashSet<>(indexFilesList);
        System.out.println(fileSet);
    // get list path data from object then convert to set
        if (rootUriString != null) {
            URI uri = URI.create(params.getRootUri());
            Path workspaceRoot = Paths.get(uri);
            groovyServices.setWorkspaceRoot(workspaceRoot,fileSet);
        }

        CompletionOptions completionOptions = new CompletionOptions(false, Arrays.asList("."));
        ServerCapabilities serverCapabilities = new ServerCapabilities();
        serverCapabilities.setCompletionProvider(completionOptions);
        serverCapabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
        serverCapabilities.setDocumentSymbolProvider(true);
        serverCapabilities.setWorkspaceSymbolProvider(true);
        serverCapabilities.setDocumentSymbolProvider(true);
        serverCapabilities.setReferencesProvider(true);
        serverCapabilities.setDefinitionProvider(true);
        serverCapabilities.setTypeDefinitionProvider(true);
        serverCapabilities.setHoverProvider(true);
        serverCapabilities.setRenameProvider(true);
        SignatureHelpOptions signatureHelpOptions = new SignatureHelpOptions();
        signatureHelpOptions.setTriggerCharacters(Arrays.asList("(", ","));
        serverCapabilities.setSignatureHelpProvider(signatureHelpOptions);

        InitializeResult initializeResult = new InitializeResult(serverCapabilities);
        return CompletableFuture.completedFuture(initializeResult);
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        return CompletableFuture.completedFuture(new Object());
    }

    @Override
    public void exit() {
        System.exit(0);
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return groovyServices;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return groovyServices;
    }

    @Override
    public void connect(LanguageClient client) {
        groovyServices.connect(client);
    }
}
