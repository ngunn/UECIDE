/*
 * Copyright (c) 2015, Majenko Technologies
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 * 
 * * Neither the name of Majenko Technologies nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.uecide;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.net.*;

import org.apache.commons.compress.archivers.ar.*;
import org.apache.commons.compress.archivers.tar.*;
import org.apache.commons.compress.compressors.gzip.*;
import org.apache.commons.compress.compressors.xz.*;
import org.apache.commons.compress.compressors.bzip2.*;

public class Package implements Comparable, Serializable {
    public HashMap<String, String> properties = new HashMap<String, String>();
    public boolean isValid = false;
    public AptPercentageListener pct = null;

    public int stateCode = 0;

    public int getState() { return stateCode; }
    public void setState(int c) { 
        stateCode = c; 
    }

    public Package(String data) {
        String[] lines = data.split("\n");
        Pattern p = Pattern.compile("^([^:]+):\\s+(.*)$", Pattern.MULTILINE);

        String currentLine = "";
        for (String line : lines) {
            if (line.startsWith(" ")) {
                currentLine += "\n";
                currentLine += line;
            } else {
                int colon = currentLine.indexOf(":");
                if (colon > 0) {
                    properties.put(currentLine.substring(0, colon), currentLine.substring(colon+2));
                }
                currentLine = line;
            }
        }
        if (!currentLine.equals("")) {
            int colon = currentLine.indexOf(":");
            if (colon > 0) {
                properties.put(currentLine.substring(0, colon), currentLine.substring(colon+2));
            }
        }

        isValid = (getName() != null);
    }
                    

    public void attachPercentageListener(AptPercentageListener listener) {
        pct = listener;
    }

    public void detachPercentageListener() {
        pct = null;
    }

    public AptPercentageListener getPercentageListener() {
        return pct;
    }

    public Package(String source, String data) {
        String[] lines = data.split("\n");
        Pattern p = Pattern.compile("^([^:]+):\\s+(.*)$", Pattern.MULTILINE);

        String currentLine = "";
        for (String line : lines) {
            if (line.startsWith(" ")) {
                currentLine += "\n";
                currentLine += line;
            } else {
                int colon = currentLine.indexOf(":");
                if (colon > 0) {
                    properties.put(currentLine.substring(0, colon), currentLine.substring(colon+2));
                }
                currentLine = line;
            }
        }
        if (!currentLine.equals("")) {
            int colon = currentLine.indexOf(":");
            if (colon > 0) {
                properties.put(currentLine.substring(0, colon), currentLine.substring(colon+2));
            }
        }

        addRepository(source);

        isValid = (getName() != null);
    }

    public void addRepository(String repo) {
        String repos = properties.get("Repository");
        if (repos == null) {
            repos = repo;
        } else {
            repos = repos + ";" + repo;
        }
        properties.put("Repository", repos);
    }

    public String toString() {
        return properties.get("Package") + " " + properties.get("Version");
    }

    public Version getVersion() {
        return new Version(properties.get("Version"));
    }

    public String getName() {
        return properties.get("Package");
    }

    public String get(String k) {
        return properties.get(k);
    }

    public int compareTo(Object o) {
        if (o instanceof Package) {
            Package op = (Package)o;
            return getDescriptionLineOne().toLowerCase().compareTo(op.getDescriptionLineOne().toLowerCase());
        }
        return 0;
    }

    public String getRepository() {
        return properties.get("Repository");
    }

    public String getDescription() {
        return properties.get("Description");
    }

    public String getDescriptionLineOne() {
        String description = properties.get("Description");
        if (description == null) {
            return "";
        }

        String lines[] = description.split("\n");
        return lines[0];
    }

    public String getInfo() {
        StringBuilder out = new StringBuilder();
        for (String k : properties.keySet()) {
            out.append(k + ": " + properties.get(k) + "\n");
        }
        return out.toString();
    }

    public String[] getReplaces() {
        String deps = properties.get("Replaces");
System.out.println("Replaces: " + deps);
        if (deps == null) {
            return null;
        }
        ArrayList<String> out = new ArrayList<String>();
        String[] spl = deps.split(",");
        for (String dep : spl) {
            out.add(dep.trim());
        }
        return out.toArray(new String[0]);
    }

    public String[] getDependencies(boolean incRec) {
        String deps = properties.get("Depends");
        if (incRec) {
            String rec = properties.get("Recommends");
            if (rec != null) {
                if (deps == null) {
                    deps = rec;
                } else {
                    deps += ", " + rec;
                }
            }
        }
        if (deps == null) {
            return null;
        }
        ArrayList<String> out = new ArrayList<String>();
        String[] spl = deps.split(",");
        for (String dep : spl) {
            out.add(dep.trim());
        }
        return out.toArray(new String[0]);
    }

    public String getSection() {
        return properties.get("Section");
    }

    public String getArchitecture() {
        return properties.get("Architecture");
    }

    public String getFilename() {
        return getName() + "_" + getVersion().toString() + "_" + getArchitecture() + ".deb";
    }

    public void shuffleArray(String[] ar) {
        Random rnd = new Random();
        for (int i = ar.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            // Simple swap
            String a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }

    public boolean fetchPackage(File folder) {
        File downloadTo = new File(folder, getFilename());
        String[] repos = properties.get("Repository").split(";");
        shuffleArray(repos);
        int contentLength = -1;
        for (String repo : repos) {
            try {
                InputStream in = null;
                if (repo.startsWith("http://") || repo.startsWith("https://")) {
                    URI uri = new URI(repo + "/" + properties.get("Filename"));
                    URL downloadFrom = uri.toURL();
                    System.out.println("Fetching " + downloadFrom);
                    HttpURLConnection httpConn = (HttpURLConnection) downloadFrom.openConnection();
                    contentLength = httpConn.getContentLength();

                    if (downloadTo.exists()) {
                        if (downloadTo.length() == contentLength) {
                            return true;
                        }
                    }
                    in = httpConn.getInputStream();
                } else if (repo.startsWith("res://")) {
                    String reps = repo.substring(6);
                    if (!reps.startsWith("/")) {
                        reps = "/" + reps;
                    }
                    in = Base.class.getResourceAsStream(reps + "/" + properties.get("Filename"));
                    if (in == null) {
                        System.err.println("Error: Resource not found: " + reps + "/" + properties.get("Filename"));
                        return false;
                    }
                } else if (repo.startsWith("file://")) {
                    return false;
                } else {
                    System.err.println("Error: No URI handler for " + repo);
                    return false;
                }

                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(downloadTo));

                byte[] buffer = new byte[1024];
                int n;
                long tot = 0;
                int lastVal = -1;
                while ((n = in.read(buffer)) > 0) {
                    tot += n;
                    if (contentLength != -1) {
                        int tpct = (int)((tot * 100) / contentLength);
                        if (tpct != lastVal) {
                            lastVal = tpct;
                            reportPercentage(tpct);
                        }
                    }
                    out.write(buffer, 0, n);
                }
                in.close();
                out.close();
                return true;
            } catch (Exception e) {
//                e.printStackTrace();
                if (downloadTo.exists()) {
                    downloadTo.delete();
                }
            }
        }
        return false;
    }

    // Extract a package and install it. Returns the control file
    // contents as a string.
    public boolean extractPackage(File cache, File db, File root) {
        String control = "";
        String md5sums = "";
        HashMap<String, Integer> installedFiles = new HashMap<String, Integer>();
        try {
            File src = new File(cache, getFilename());
            if (!src.exists()) {
                System.err.println("Unable to open cache file");
                return false;
            }

            System.out.println("Extracting " + getFilename());

            FileInputStream fis = new FileInputStream(src);
            ArArchiveInputStream ar = new ArArchiveInputStream(fis);

            ArArchiveEntry file = ar.getNextArEntry();
            int dataFileSize = 0;
            while (file != null) {
                long size = file.getSize();
                String name = file.getName();


                if (name.equals("control.tar.gz")) {
                    GzipCompressorInputStream gzip = new GzipCompressorInputStream(ar);
                    TarArchiveInputStream tar = new TarArchiveInputStream(gzip);
                    TarArchiveEntry te = tar.getNextTarEntry();
                    while (te != null) {
                        int tsize = (int)te.getSize();
                        String tname = te.getName();
                        if (tname.equals("./control")) {
                            byte[] data = new byte[tsize];
                            tar.read(data, 0, tsize);
                            control = new String(data, "UTF-8");
                            String[] lines = control.split("\n");
                            for (String line : lines) {
                                if (line.startsWith("Installed-Size: ")) {
                                    String iss = line.substring(16);
                                    dataFileSize = Integer.parseInt(iss) * 1024;
                                }
                            }
                        }
                        if (tname.equals("./md5sums")) {
                            byte[] data = new byte[tsize];
                            tar.read(data, 0, tsize);
                            md5sums = new String(data, "UTF-8");
                        }
                        te = tar.getNextTarEntry();
                    }

                }

                if (name.equals("data.tar.gz")) {
                    GzipCompressorInputStream gzip = new GzipCompressorInputStream(ar);
                    TarArchiveInputStream tar = new TarArchiveInputStream(gzip);
                    installedFiles = extractTarFile(tar, root, dataFileSize);
                }
                    
                if (name.equals("data.tar.xz")) {
                    XZCompressorInputStream xzip = new XZCompressorInputStream(ar);
                    TarArchiveInputStream tar = new TarArchiveInputStream(xzip);
                    installedFiles = extractTarFile(tar, root, dataFileSize);
                }

                if (name.equals("data.tar.bz2")) {
                    BZip2CompressorInputStream bzip = new BZip2CompressorInputStream(ar);
                    TarArchiveInputStream tar = new TarArchiveInputStream(bzip);
                    installedFiles = extractTarFile(tar, root, dataFileSize);
                }

                file = ar.getNextArEntry();
            }


            ar.close();
            fis.close();
            

            File pf = new File(db, getName());
            pf.mkdirs();
            File cf = new File(pf, "control");
            PrintWriter pw = new PrintWriter(cf);
            pw.println(control);
            pw.close();
            File mf = new File(pf, "md5sums");
            pw = new PrintWriter(mf);
            pw.println(md5sums);
            pw.close();
            File ff = new File(pf, "files");
            pw = new PrintWriter(ff);
            for (String f : installedFiles.keySet()) {
                pw.println(f);
            }
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        
        return true;
    }

    void reportPercentage(int p) {
        if (pct != null) {
            if (p < 0) {
                p = 0;
            }
            if (p > 100) {
                p = 100;
            }
            pct.updatePercentage(this, p);
        }
    }

    HashMap<String, Integer> extractTarFile(TarArchiveInputStream tar, File root, int dataFileSize) {
        HashMap<String, Integer> installedFiles = new HashMap<String, Integer>();
        HashMap<String, String> symbolicLinks = new HashMap<String, String>();
        try {
            TarArchiveEntry te = tar.getNextTarEntry();

            int copied = 0;
            while (te != null) {
                int tsize = (int)te.getSize();
                copied += tsize;
                String tname = te.getName();
                if (pct != null) {
                    int tpct = (int)((copied * 100L) / dataFileSize);
                    reportPercentage(tpct);
                }

                File dest = new File(root, tname);
                if (te.isDirectory()) {
                    dest.mkdirs();
                    installedFiles.put(dest.getAbsolutePath(), -1);
                } else if (te.isLink()) {
                    String linkdest = te.getLinkName();
                    symbolicLinks.put(tname, linkdest);
                } else if (te.isSymbolicLink()) {
                    String linkdest = te.getLinkName();
                    symbolicLinks.put(tname, linkdest);
                } else {
                    byte[] buffer = new byte[1024];
                    int nread;
                    int toRead = tsize;
                    FileOutputStream fos = new FileOutputStream(dest);
                    while ((nread = tar.read(buffer, 0, toRead > 1024 ? 1024 : toRead)) > 0) {
                        toRead -= nread;
                        fos.write(buffer, 0, nread);
                    }
                    fos.close();
                    dest.setExecutable((te.getMode() & 0100) == 0100);
                    dest.setWritable((te.getMode() & 0200) == 0200);
                    dest.setReadable((te.getMode() & 0400) == 0400);
                    installedFiles.put(dest.getAbsolutePath(), tsize);
                }
                te = tar.getNextTarEntry();
            }
            for (String link : symbolicLinks.keySet()) {
                String tgt = symbolicLinks.get(link);
                File linkFile = new File(root, link);
                File linkParent = linkFile.getParentFile();
                File tgtFile = new File(linkParent, tgt);
                FileInputStream copyFrom = new FileInputStream(tgtFile);
                FileOutputStream copyTo = new FileOutputStream(linkFile);
                byte[] copyBuffer = new byte[1024];
                int bytesCopied = 0;

                while ((bytesCopied = copyFrom.read(copyBuffer, 0, 1024)) > 0) {
                    copyTo.write(copyBuffer, 0, bytesCopied);
                }
                copyFrom.close();
                copyTo.close();
                linkFile.setExecutable(tgtFile.canExecute());
                linkFile.setReadable(tgtFile.canRead());
                linkFile.setWritable(tgtFile.canWrite());
            }
        } catch (Exception e) {
            Base.error(e);
        }
        return installedFiles;
    }

}
