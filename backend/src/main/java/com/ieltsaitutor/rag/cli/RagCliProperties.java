package com.ieltsaitutor.rag.cli;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag")
public class RagCliProperties {
    private String cli = "";
    private String manifest = "backend/rag-data/manifest.yml";
    public String getCli() { return cli; }
    public void setCli(String cli) { this.cli = cli == null ? "" : cli; }
    public String getManifest() { return manifest; }
    public void setManifest(String manifest) { this.manifest = manifest; }
}
