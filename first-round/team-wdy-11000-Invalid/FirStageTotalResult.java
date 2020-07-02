package com.kuaishou.kcode.model;

/**
 * @author wdy
 * @create 2020-06-25 11:37
 */
public class FirStageTotalResult {
    private FirStageResult[] firStageTotalResult;

    public FirStageTotalResult(int num) {
        firStageTotalResult = new FirStageResult[num];
    }

    public FirStageResult[] getFirStageTotalResult() {
        return firStageTotalResult;
    }

    public void setFirStageTotalResult(FirStageResult[] firStageTotalResult) {
        this.firStageTotalResult = firStageTotalResult;
    }
}
