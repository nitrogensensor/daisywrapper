package eu.nitrogensensor.executionservice;

import eu.nitrogensensor.daisylib.DaisyModel;
import eu.nitrogensensor.daisylib.ResultExtractor;

import java.util.ArrayList;

public class ExecutionBatch {
    public String oploadId;
    public ResultExtractor resultExtractor;
    public DaisyModel kørsel;

    @Override
    public String toString() {
        return "ExecutionBatch{" +
                "oploadId='" + oploadId + '\'' +
                ", resultExtractor=" + resultExtractor +
                ", kørsel=" + kørsel +
                '}';
    }
}
