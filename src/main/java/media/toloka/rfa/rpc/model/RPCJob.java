package media.toloka.rfa.rpc.model;

//import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.gson.annotations.Expose;
import lombok.Data;
import lombok.ToString;
import media.toloka.rfa.security.model.Users;

import java.util.*;

@Data
@ToString
public class RPCJob {
    @Expose
    private ERPCJobType rJobType;
    //@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @Expose
    private Date rjobdate;
    @Expose
    private Users user;
    @Expose
    private Queue<ERPCJobType> jobchain;
    @Expose
    private String rjobdata;
    @Expose
    private List<ResultJob> resultJobList = new ArrayList<>();

    public RPCJob() {
        this.rjobdate = new Date();
        this.jobchain = new LinkedList<ERPCJobType>();
    }
}
