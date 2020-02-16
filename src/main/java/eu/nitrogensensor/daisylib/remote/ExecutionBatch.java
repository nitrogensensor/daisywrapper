package eu.nitrogensensor.daisylib.remote;

import eu.nitrogensensor.daisylib.DaisyModel;
import eu.nitrogensensor.daisylib.ResultExtractor;

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
