/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sum;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.hadoop.io.Writable;

/**
 *
 * @author anike
 */
public class Tuple implements Writable {
    
    private Date first = new Date();
    private Date last = new Date();
    private long count = 0;
    private SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy");

    public Date getFirst() {
        return first;
    }

    public void setFirst(Date first) {
        this.first = first;
    }

    public Date getLast() {
        return last;
    }

    public void setLast(Date last) {
        this.last = last;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    @Override
    public String toString() {
        if((first != null) && (last !=null)){
        return fmt.format(first) + "\t" + fmt.format(last) + "\t" + count;
        }
        return "";
    }
    
    @Override
        public void readFields(DataInput in) throws IOException {
            first = new Date(in.readLong());
            last = new Date(in.readLong());
            count = in.readLong();
            
            
        }

        @Override
        public void write(DataOutput out) throws IOException {
            out.writeLong(first.getTime());
            out.writeLong(last.getTime());
            out.writeLong(count);
        }
}
