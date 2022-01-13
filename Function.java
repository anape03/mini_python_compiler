public class Function {
    private String name;
    private int params;
    private int defaultParams;

    public Function(String name) {
        this.name = name;
        params = 0;
        defaultParams = 0;
    }

    public Function(String name, int params, int defaultParams) {
        this(name);
        this.params = params;
        this.defaultParams = defaultParams;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getParams() {
        return params;
    }

    public void setParams(int params) {
        this.params = params;
    }

    public int getDefaultParams() {
        return defaultParams;
    }

    public void setDefaultParams(int defaultParams) {
        this.defaultParams = defaultParams;
    }

    public int getTotalParams() {
        return defaultParams + params;
    }

    public String toString() {
        return name + " " + params + " " + defaultParams;
    }
}
