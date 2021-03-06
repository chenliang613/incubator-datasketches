/*
 * Copyright 2019, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.docgen;

import static com.yahoo.sketches.Files.openPrintWriter;

import java.io.File;
import java.io.PrintWriter;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.yahoo.sketches.Files;

/**
 * @author Lee Rhodes
 */
public class TocGenerator {
  private static final String LS = System.getProperty("line.separator");
  private int level = 0;
  private PrintWriter pw = null;
  private String jsonScrFile;
  private String htmlScriptFile;

  /**
   *
   * @param jsonSrcFile The JSON source file
   * @param htmlScriptFile The javascript source file
   * @param tgtTocFile the target toc.html file
   */
  public TocGenerator(final String jsonSrcFile, final String htmlScriptFile, final String tgtTocFile) {
    jsonScrFile = jsonSrcFile;
    this.htmlScriptFile = htmlScriptFile;
    if ((tgtTocFile != null) && (!tgtTocFile.isEmpty())) {
      final File file = new File(tgtTocFile);
      if (file.exists()) { file.delete(); }
      pw = openPrintWriter(tgtTocFile);
    }
  }

  /**
   * Reads the JSON source file and the html script file and generates the target toc.html file.
   */
  public void readJson() {
    final StringBuilder sb = new StringBuilder();
    final String jin = Files.fileToString(jsonScrFile);
    final JSONObject jo = new JSONObject(jin);
    final String clazz = jo.getString("class");
    if (clazz.equals("TOC")) { emitToc(jo, sb); }
    else if (clazz.equals("Dropdown")) { emitDropdown(jo, sb); }
    else { emitDoc(jo, sb); }
    if ((htmlScriptFile != null) && (!htmlScriptFile.isEmpty())) {
      final String script = Files.fileToString(htmlScriptFile);
      sb.append(script);
    }
    println(sb.toString());
  }

  /**
   * Generates an entire toc.html in a StringBuilder
   * @param toc the input JSON object
   * @param sb the target StringBuilder
   */
  void emitToc(final JSONObject toc, final StringBuilder sb) {
    sb.append("<!-- Computer Generated File, Do Not Edit! -->").append(LS);
    sb.append("<link rel=\"stylesheet\" href=\"/css/toc.css\">").append(LS);
    sb.append("<div id=\"toc\" class=\"nav toc hidden-print\">").append(LS);

    //JSONArray
    level++;
    final JSONArray jarr = toc.getJSONArray("array");
    final Iterator<Object> itr = jarr.iterator();
    while (itr.hasNext()) {
      final JSONObject jo = (JSONObject) itr.next();
      final String clazz = jo.getString("class");
      if (clazz.equals("Dropdown")) { emitDropdown(jo, sb); }
      else { emitDoc(jo, sb); }
    }
    level--;

    sb.append("</div>").append(LS);
  }

  /**
   * Generates a Dropdown object from the input JSON source.
   * This is a recursive co-routine with the emitDoc() function.
   * @param dropdn the input JSON
   * @param sb the target StringBuilder
   */
  void emitDropdown(final JSONObject dropdn, final StringBuilder sb) {
    final String desc = dropdn.getString("desc");
    final String lowercaseDesc = desc.toLowerCase();
    final String pId = lowercaseDesc.replace(' ', '-');
    final String divId = "collapse_" + lowercaseDesc.replace(' ', '_');
    final String href = "#" + divId;
    final String indent = indent(level);
    //paragraph with desc
    sb.append(LS);
    sb.append(indent).append("<p id=").append(quotes(pId)).append(">").append(LS);
    sb.append(indent).append("  ").append("<a data-toggle=\"collapse\" ")
      .append("class=\"menu collapsed\" href=").append(quotes(href)).append(">")
      .append(desc).append("</a>").append(LS);
    sb.append(indent).append("</p>").append(LS);
    //start dropdown array
    sb.append(indent).append("<div class=\"collapse\" ").append("id=").append(quotes(divId))
      .append(">").append(LS);

    //JSONArray
    level++;
    final JSONArray jarr = dropdn.getJSONArray("array");
    final Iterator<Object> itr = jarr.iterator();
    while (itr.hasNext()) {
      final JSONObject jo = (JSONObject) itr.next();
      final String clazz = jo.getString("class");
      if (clazz.equals("Dropdown")) { emitDropdown(jo, sb); }
      else { emitDoc(jo, sb); }
    }
    level--;

    sb.append(indent).append("</div>").append(LS);
  }

  /**
   * Generates a Document object from the input JSON source.
   * This is a recursive co-routine with the emitDropdown() function.
   * @param doc the input JSON
   * @param sb the target StringBuilder
   */
  void emitDoc(final JSONObject doc, final StringBuilder sb) {
    final String dir = doc.getString("dir");
    final String file = doc.getString("file");
    final String desc = doc.getString("desc");
    final boolean pdf = doc.optBoolean("pdf");
    final String indent = indent(level);
    sb.append(indent).append("<li><a href=\"");
    if (dir.equals("ROOT")) { sb.append("/"); }
    else {
      final String baseDir = pdf ? "{{site.docs_pdf_dir}}/" : "{{site.docs_dir}}/";
      sb.append(baseDir);
      if (!dir.isEmpty()) {
        sb.append(dir + "/");
      }
    }
    sb.append(file);
    final String sfx = pdf ? ".pdf" : ".html";
    sb.append(sfx + "\">");
    sb.append(desc);
    sb.append("</a></li>").append(LS);
  }

  /**
   * Encases the given string in quotes.
   * @param s the given string
   * @return the quoted string
   */
  public static String quotes(final String s) {
    return '"' + s + '"';
  }

  /**
   * @param level indention level
   * @return the indention spaces
   */
  public static String indent(final int level) {
    assert level >= 0;
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < level; i++) {
      sb.append("  ");
    }
    return sb.toString();
  }


  /**
   * The JVM may call this method to close the PrintWriter resource.
   */
  @Override
  protected void finalize() throws Throwable {
    try {
      if (pw != null) {
        pw.close(); // close open files
      }
    } finally {
      super.finalize();
    }
  }

  /**
   * Outputs a line to the configured PrintWriter and stdOut.
   * @param s The String to print
   */
  public final void println(final String s) {
    System.out.println(s);
    if (pw != null) {
      pw.println(s);
      pw.flush();
    }
  }

  /**
   * Flush any buffered output to the configured PrintWriter.
   */
  public final void flush() {
    if (pw != null) { pw.flush(); }
  }

  /**
   * Command line access.
   * @param args three arguments are required:
   * <ol><li>The JSON source file</li>
   * <li>The html script file that is appended to the end.</li>
   * <li>The target toc.html file</li>
   * </ol>
   */
  public static void main(final String[] args) {
    final String jsonSrcFile = args[0];
    final String htmlScriptFile = args[1];
    final String tgtTocFile = args[2];
    final TocGenerator tocgen = new TocGenerator(jsonSrcFile, htmlScriptFile, tgtTocFile);
    tocgen.readJson();
  }

}
