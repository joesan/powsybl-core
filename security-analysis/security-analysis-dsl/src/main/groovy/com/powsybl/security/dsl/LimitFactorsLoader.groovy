package com.powsybl.security.dsl


import com.powsybl.dsl.DslException
import com.powsybl.dsl.DslLoader
import com.powsybl.dsl.ExpressionDslLoader
import com.powsybl.dsl.ast.ExpressionNode
import com.powsybl.security.LimitViolationDetector
import com.powsybl.security.dsl.ast.LimitsVariableNode
import org.codehaus.groovy.control.CompilationFailedException

import java.util.function.Consumer

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
class LimitFactorsLoader extends DslLoader {


    LimitFactorsLoader(GroovyCodeSource dslSrc) {
        super(dslSrc)
    }

    LimitFactorsLoader(File dslFile) {
        super(dslFile)
    }

    LimitFactorsLoader(String script) {
        super(script)
    }

    static class LimitSpec {

        /**
         * Variable which may be referenced as "voltage" from the DSL
         */
        static LimitsVariableNode voltage = LimitsVariableNode.voltage()
        /**
         * Variable which may be referenced as "duration" from the DSL
         */
        static LimitsVariableNode duration =  LimitsVariableNode.duration()

        LimitFactorsNode node

        LimitSpec(LimitMatcher selector) {
            this.node = new ConditionNode(selector)
        }

        LimitSpec() {
            this.node = new LimitFactorsNode()
        }

        void where(ExpressionNode node, @DelegatesTo(LimitSpec) Closure cl) {
            executeClosure new ExpressionLimitMatcher(node), cl
        }

        void factor(float f) {
            node.addChild(new FinalFactorNode(f))
        }

        void executeClosure(LimitMatcher selector, @DelegatesTo(LimitSpec) Closure cl) {
            def cloned = cl.clone()
            cloned.resolveStrategy = Closure.DELEGATE_FIRST

            LimitSpec enclosedSpec = new LimitSpec(selector)
            cloned.delegate = enclosedSpec
            cloned()
            node.addChild(enclosedSpec.node)
        }

        void temporary(@DelegatesTo(LimitSpec) Closure cl) {
            executeClosure LimitMatchers.TEMPORARY, cl
        }

        void permanent(@DelegatesTo(LimitSpec) Closure cl) {
            executeClosure LimitMatchers.PERMANENT, cl
        }

        void N_situation(@DelegatesTo(LimitSpec) Closure cl) {
            executeClosure LimitMatchers.N_SITUATION, cl
        }

        void any_contingency( @DelegatesTo(LimitSpec) Closure cl) {
            executeClosure LimitMatchers.ANY_CONTINGENCY, cl
        }

        void contingency(String id, @DelegatesTo(LimitSpec) Closure cl) {
            executeClosure LimitMatchers.contingency(id), cl
        }

        void contingencies(Collection<String> ids, @DelegatesTo(LimitSpec) Closure cl) {
            executeClosure LimitMatchers.contingencies(ids), cl
        }

        void branch(String id, @DelegatesTo(LimitSpec) Closure cl) {
            executeClosure LimitMatchers.branch(id), cl
        }

        void branches(Collection<String> ids, @DelegatesTo(LimitSpec) Closure cl) {
            executeClosure LimitMatchers.branches(ids), cl
        }
    }

    static LimitFactors createFactors(@DelegatesTo(LimitSpec) Closure cl) {
        def cloned = cl.clone()
        cloned.resolveStrategy = Closure.DELEGATE_FIRST

        LimitSpec spec = new LimitSpec()
        cloned.delegate = spec
        cloned()

        return spec.node
    }

    static void loadDsl(Binding binding, Consumer<LimitFactors> handler) {

        ExpressionDslLoader.prepareClosures(binding)

        binding.currentLimits = { Closure cl -> handler.accept(createFactors(cl)) }

    }

    LimitFactors loadFactors() {

        LimitFactors root = new LimitFactorsNode()

        try {
            Binding binding = new Binding()

            loadDsl(binding, root.&addChild)

            def shell = createShell(binding)

            shell.evaluate(dslSrc)

            root

        } catch (CompilationFailedException e) {
            throw new DslException(e.getMessage(), e)
        }
    }

    LimitViolationDetector loadDetector() {
        return new LimitViolationDetectorWithFactors(loadFactors())
    }
}
