package Trees;

import Actors.Operation;
import Actors.Provider;
import Statistics.Metrics.BasicMetric;
import Statistics.Metrics.CostMetric;
import Statistics.Metrics.ProviderMetric;
import Trees.Semantics.Features;

public class TreeNodeCostEngine {


    /**
     * NB the cost engine does not apply/correct operations features status
     */
    public static <T extends Operation> CostMetric computeCost(TreeNode<T> tn, Provider candidate, Features f) {
        CostMetric m = new CostMetric();
        m.setAllZero();

        for (TreeNode<T> tns : tn.getSons()
                ) {
            /**
             * Sums the cost of all sons
             */
            m.Ct += tns.getElement().getCost().Ct;

            /**
             * Compute encryption cost
             */
            //case no encrypted data -> encryption
            if (tns.getInfo().hasFeature(Features.NOTENCRYPTED)) {
                //case symmetric
                if (f == Features.ENCRYPTEDSYM) {
                    m.Cc += tns.getElement().getOp_metric().outputSize * tns.getElement().getExecutor().getMetrics().Kc1;
                }
                //case homomorphic
                else if (f == Features.ENCRYPTEDHOM) {
                    m.Cc += tns.getElement().getOp_metric().outputSize * tns.getElement().getExecutor().getMetrics().Kc2;
                }
            }
            //case encrypted -> no encryption
            else if (f == Features.NOTENCRYPTED) {
                //case decrypt asymmetric
                if (tns.getInfo().hasFeature(Features.ENCRYPTEDSYM)) {
                    m.Cc += tns.getElement().getOp_metric().outputSize * candidate.getMetrics().Kc1;
                }
                //case decrypt homomorphic
                else if (tns.getInfo().hasFeature(Features.ENCRYPTEDHOM)) {
                    m.Cc += tns.getElement().getOp_metric().outputSize * candidate.getMetrics().Kc2;
                }
            }
            //case wrong encryption double cost
            else {
                m.Cc += tns.getElement().getOp_metric().outputSize * (candidate.getMetrics().Kc2
                        + candidate.getMetrics().Kc1);
            }

            /**
             * Compute motion cost
             */
            if (!tns.getElement().getExecutor().selfDescription().equals(candidate.selfDescription())) {
                m.Cm += tns.getElement().getOp_metric().outputSize * (tns.getElement().getExecutor().getMetrics().Km + candidate.getMetrics().Km);
            }

        }//end foreach son

        /**
         * Compute execution cost
         */
        m.Ce += tn.getElement().getOp_metric().CPU_time * candidate.getMetrics().Kcpu + tn.getElement().getOp_metric().IO_time * candidate.getMetrics().Kio;

        /**
         * Cost summation
         */
        m.Ct += m.Ce + m.Cc + m.Cm;

        return m;
    }

    public static <T extends Operation> boolean validateCost(TreeNode<T> tn) {
        boolean flag = false;
        if (tn.getElement().getCost().Ct == tn.getOracle().getElement().getCost().Ct
                && (tn.getOracle().getElement().getExecutor().selfDescription().equals(tn.getElement().getExecutor().selfDescription())))
            return true;
        if (tn.getElement().getCost().Ct < tn.getOracle().getElement().getCost().Ct) {
            //retrieve oracle's information
            CostMetric m1 = tn.getOracle().getElement().getCost();
            //build new incremental information
            CostMetric m2 = new CostMetric();
            m2.setAllZero();
            //retrieve operation + executor metrics
            ProviderMetric pm = tn.getElement().getExecutor().getMetrics();
            BasicMetric op = tn.getElement().getOp_metric();
            //compute new incremental metrics
            //execution cost
            m2.Ce = op.CPU_time * pm.Kcpu + op.IO_time * pm.Kio;
            //motion cost
            if (!(tn.getOracle().getElement().getExecutor().selfDescription().equals(tn.getElement().getExecutor().selfDescription())))
                m2.Cm = op.outputSize * pm.Km;
            //encryption cost
            if (tn.getInfo().hasFeature(Features.ENCRYPTEDSYM))
                m2.Cc = op.outputSize * tn.getOracle().getElement().getExecutor().getMetrics().Kc1;
            else if (tn.getInfo().hasFeature(Features.ENCRYPTEDHOM))
                m2.Cc = op.outputSize * tn.getOracle().getElement().getExecutor().getMetrics().Kc2;
            //cost comparison
            if (m1.getIncrCost() > m2.getIncrCost())
                return true;
        }
        return flag;

    }

}
