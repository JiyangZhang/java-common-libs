/*
 * Copyright 2013-2016 Raffael Herzog, Marko Umek
 *
 * This file is part of markdown-doclet.
 *
 * markdown-doclet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * markdown-doclet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with markdown-doclet.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.opencb.commons.docs;

import com.sun.javadoc.DocErrorReporter;
import com.sun.tools.doclets.standard.Standard;

import java.io.*;
import java.util.*;


/**
 * Processes and stores the command line options.
 *
 * @author Juanfe
 */
public class Options {

    public static final String OPT_OUTPUT_DIR = "-outputdir";
    public static final String OPT_CLASSES_TO_MARKDOWN = "-classes2Markdown";
    public static final String OPT_JSON_DIR = "-jsondir";
    public static final String OPT_GITHUB_SERVER = "-githubServer";
    public static final String OPT_TABLE_TAGS_CLASSES = "-tableTagsClasses";


    private String githubServer = "https://github.com/opencb/opencga/blob/master/opencga-core/";
    private List<String> classes2Markdown = new ArrayList<>();


    private List<String> tableTagsClasses = new ArrayList<>();


    private String outputdir;

    private Map<String, String> jsonMap = new HashMap<>();

    public Options() {
    }


    /**
     * Loads the options from the command line.
     *
     * @param options The command line options.
     */
    public void load(String[][] options) {
        for (int i = 0; i < options.length; i++) {
            String[] opt = options[i];
            if (OPT_OUTPUT_DIR.equals(opt[0])) {
                if (!opt[1].endsWith(File.separator)) {
                    opt[1] = opt[1] + File.separator;
                }
                setOutputdir(opt[1]);
            } else if (OPT_CLASSES_TO_MARKDOWN.equals(opt[0])) {
                String[] aux = opt[1].split(";");
                setClasses2Markdown(Arrays.asList(aux));
            } else if (OPT_JSON_DIR.equals(opt[0])) {
                if (!opt[1].endsWith(File.separator)) {
                    opt[1] = opt[1] + File.separator;
                }
                loadJsonMap(opt[1]);
            } else if (OPT_GITHUB_SERVER.equals(opt[0])) {
                if (!opt[1].endsWith(File.separator)) {
                    opt[1] = opt[1] + File.separator;
                }
                setGithubServer(opt[1]);
            } else if (OPT_TABLE_TAGS_CLASSES.equals(opt[0])) {
                String[] aux = opt[1].split(";");
                setTableTagsClasses(Arrays.asList(aux));
            }
        }

    }

    /**
     * Loads the jsonMap listing json folder and store in map the files content as Strings.
     *
     * @param jsondir The json folder passed as option.
     */
    private void loadJsonMap(String jsondir) {
        File folder = new File(jsondir);
        System.out.println("The folder " + jsondir);
        File[] files = folder.listFiles();
        if (files != null) {
            for (final File fileEntry : files) {
                jsonMap.put(fileEntry.getName(), getFileContentAsString(fileEntry.getAbsolutePath()));
            }
        }
    }

    /**
     * Read file content and returns it as Strings.
     *
     * @param path The file path.
     * @return the content of the file as string
     */
    private String getFileContentAsString(String path) {
        String res = "";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            StringBuilder out = new StringBuilder();
            String line = null;

            while ((line = reader.readLine()) != null) {
                out.append(line + "\n");

            }
            reader.close();
            res = out.toString();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * Checks the length of the option.
     *
     * @param option The option to check.
     * @return the valid length of this option
     */
    public static int optionLength(String option) {
        int res = Standard.optionLength(option);
        switch (option) {
            case OPT_CLASSES_TO_MARKDOWN:
            case OPT_OUTPUT_DIR:
            case OPT_JSON_DIR:
            case OPT_GITHUB_SERVER:
            case OPT_TABLE_TAGS_CLASSES:
                res = 2;
                break;
            default:
                break;
        }

        return res;
    }

    /**
     * Checks the content of the option.
     *
     * @param options The options to check.
     * @param reporter The inherited reporter param.
     * @return if are all content options valid
     */
    public static boolean validOptions(String[][] options, DocErrorReporter reporter) {
        boolean foundOutputdirOption = false;
        for (int i = 0; i < options.length; i++) {
            String[] opt = options[i];
            if ("-outputdir".equals(opt[0]) && opt[1] != null && opt[1].length() > 0) {
                foundOutputdirOption = true;
            }
        }

        boolean foundclasses2MarkdownOption = false;
        for (int i = 0; i < options.length; i++) {
            String[] opt = options[i];
            if ("-classes2Markdown".equals(opt[0]) && opt[1] != null && opt[1].length() > 0) {
                foundclasses2MarkdownOption = true;
            }
        }
        if (!foundOutputdirOption) {
            reporter.printError("The output dir for md files is a mandatory option");
        }

        if (!foundclasses2MarkdownOption) {
            reporter.printError("The option classes2Markdown is mandatory");
        }
        return foundOutputdirOption && foundclasses2MarkdownOption;
    }

    public String getOutputdir() {
        return outputdir;
    }

    public void setOutputdir(String outputdir) {
        this.outputdir = outputdir;
    }


    public List<String> getClasses2Markdown() {
        return classes2Markdown;
    }

    public void setClasses2Markdown(List<String> classes2Markdown) {
        this.classes2Markdown = classes2Markdown;
    }

    public Map<String, String> getJsonMap() {
        return jsonMap;
    }

    public void setJsonMap(Map<String, String> jsonMap) {
        this.jsonMap = jsonMap;
    }

    public String getGithubServer() {
        return githubServer;
    }

    public void setGithubServer(String githubServer) {
        this.githubServer = githubServer;
    }

    public List<String> getTableTagsClasses() {
        return tableTagsClasses;
    }

    public void setTableTagsClasses(List<String> tableTagsClasses) {
        this.tableTagsClasses = tableTagsClasses;
    }

}
