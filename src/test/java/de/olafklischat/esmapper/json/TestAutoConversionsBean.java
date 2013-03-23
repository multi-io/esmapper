package de.olafklischat.esmapper.json;

public class TestAutoConversionsBean {
    
    private int primInt;
    private long primLong;
    private float primFloat;
    private double primDouble;
    private Integer objInt;
    private Long objLong;
    private Float objFloat;
    private Double objDouble;
    public TestAutoConversionsBean() {
    }
    public TestAutoConversionsBean(int primInt, long primLong, float primFloat,
            double primDouble, Integer objInt, Long objLong, Float objFloat,
            Double objDouble) {
        super();
        this.primInt = primInt;
        this.primLong = primLong;
        this.primFloat = primFloat;
        this.primDouble = primDouble;
        this.objInt = objInt;
        this.objLong = objLong;
        this.objFloat = objFloat;
        this.objDouble = objDouble;
    }
    public int getPrimInt() {
        return primInt;
    }
    public void setPrimInt(int primInt) {
        this.primInt = primInt;
    }
    public long getPrimLong() {
        return primLong;
    }
    public void setPrimLong(long primLong) {
        this.primLong = primLong;
    }
    public float getPrimFloat() {
        return primFloat;
    }
    public void setPrimFloat(float primFloat) {
        this.primFloat = primFloat;
    }
    public double getPrimDouble() {
        return primDouble;
    }
    public void setPrimDouble(double primDouble) {
        this.primDouble = primDouble;
    }
    public Integer getObjInt() {
        return objInt;
    }
    public void setObjInt(Integer objInt) {
        this.objInt = objInt;
    }
    public Long getObjLong() {
        return objLong;
    }
    public void setObjLong(Long objLong) {
        this.objLong = objLong;
    }
    public Float getObjFloat() {
        return objFloat;
    }
    public void setObjFloat(Float objFloat) {
        this.objFloat = objFloat;
    }
    public Double getObjDouble() {
        return objDouble;
    }
    public void setObjDouble(Double objDouble) {
        this.objDouble = objDouble;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((objDouble == null) ? 0 : objDouble.hashCode());
        result = prime * result
                + ((objFloat == null) ? 0 : objFloat.hashCode());
        result = prime * result + ((objInt == null) ? 0 : objInt.hashCode());
        result = prime * result + ((objLong == null) ? 0 : objLong.hashCode());
        long temp;
        temp = Double.doubleToLongBits(primDouble);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + Float.floatToIntBits(primFloat);
        result = prime * result + primInt;
        result = prime * result + (int) (primLong ^ (primLong >>> 32));
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TestAutoConversionsBean other = (TestAutoConversionsBean) obj;
        if (objDouble == null) {
            if (other.objDouble != null)
                return false;
        } else if (!objDouble.equals(other.objDouble))
            return false;
        if (objFloat == null) {
            if (other.objFloat != null)
                return false;
        } else if (!objFloat.equals(other.objFloat))
            return false;
        if (objInt == null) {
            if (other.objInt != null)
                return false;
        } else if (!objInt.equals(other.objInt))
            return false;
        if (objLong == null) {
            if (other.objLong != null)
                return false;
        } else if (!objLong.equals(other.objLong))
            return false;
        if (Double.doubleToLongBits(primDouble) != Double
                .doubleToLongBits(other.primDouble))
            return false;
        if (Float.floatToIntBits(primFloat) != Float
                .floatToIntBits(other.primFloat))
            return false;
        if (primInt != other.primInt)
            return false;
        if (primLong != other.primLong)
            return false;
        return true;
    }
}
