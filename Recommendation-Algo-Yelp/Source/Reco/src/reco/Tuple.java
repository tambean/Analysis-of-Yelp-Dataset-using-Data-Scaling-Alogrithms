/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reco;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableUtils;

/**
 *
 * @author anike
 */
public class Tuple implements Writable {
    

    private String businessid;
    private double rating;

    public String getBusinessid() {
        return businessid;
    }

    public void setBusinessid(String businessid) {
        this.businessid = businessid;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }
    
    @Override
        public void readFields(DataInput in) throws IOException {
            businessid = WritableUtils.readString(in);
            rating = in.readDouble();
            
        }

        @Override
        public void write(DataOutput out) throws IOException {
            WritableUtils.writeString(out, businessid);
            out.writeDouble(rating);
        }

        @Override
        public String toString() {
            return businessid + "\t" + rating;
        }
    
    
    
    
}
