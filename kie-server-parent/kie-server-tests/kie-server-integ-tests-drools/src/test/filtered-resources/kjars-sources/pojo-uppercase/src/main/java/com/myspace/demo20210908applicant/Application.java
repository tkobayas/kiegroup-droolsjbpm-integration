package com.myspace.demo20210908applicant;

/**
 * This class was automatically generated by the data modeler tool.
 */

public class Application implements java.io.Serializable {

    static final long serialVersionUID = 1L;

    @org.kie.api.definition.type.Label("family")
    private java.util.List<com.myspace.demo20210908applicant.Applicant> family;

    @org.kie.api.definition.type.Label(value = "Program Name")
    private java.lang.String programName;

    public Application() {}

    public java.util.List<com.myspace.demo20210908applicant.Applicant> getFamily() {
        return this.family;
    }

    public void setFamily(
                          java.util.List<com.myspace.demo20210908applicant.Applicant> family) {
        System.out.println(family);
        this.family = family;
    }

    public java.lang.String getProgramName() {
        return this.programName;
    }

    public void setProgramName(java.lang.String programName) {
        this.programName = programName;
    }

    public Application(
                       java.util.List<com.myspace.demo20210908applicant.Applicant> family,
                       java.lang.String programName) {
        this.family = family;
        this.programName = programName;
    }
    @Override
    public String toString() {
        return "Application [family=" + family + ", programName=" + programName + "]";
    }
}