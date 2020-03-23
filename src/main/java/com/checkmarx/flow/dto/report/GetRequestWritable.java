package com.checkmarx.flow.dto.report;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;


@EqualsAndHashCode(callSuper = true)
@Data
public class GetRequestWritable extends Writable{

    public static final String OPERATION = "Get Request";

    public GetRequestWritable(Integer scanId, ScanRequest request) {
        super(scanId,request);
    }

    public GetRequestWritable(String scanId, ScanRequest request) {
        super(scanId,request);
    }
    
    @Override
    public String _getOperation() {
        return OPERATION;
    }
    
}
