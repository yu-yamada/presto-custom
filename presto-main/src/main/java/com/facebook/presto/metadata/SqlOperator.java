/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.metadata;

import com.facebook.presto.operator.scalar.ScalarFunctionImplementation;
import com.facebook.presto.spi.type.TypeManager;
import com.facebook.presto.spi.type.TypeSignature;
import com.google.common.collect.ImmutableList;

import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.facebook.presto.metadata.FunctionRegistry.mangleOperatorName;
import static java.util.Objects.requireNonNull;

public abstract class SqlOperator
        extends SqlScalarFunction
{
    public static SqlOperator create(
            OperatorType operatorType,
            List<TypeVariableConstraint> typeVariableConstraints,
            List<LongVariableConstraint> longVariableConstraints,
            List<TypeSignature> argumentTypes,
            TypeSignature returnType,
            MethodHandle methodHandle,
            Optional<MethodHandle> instanceFactory,
            boolean nullable,
            List<Boolean> nullableArguments,
            Set<String> literalParameters)
    {
        List<String> arugmentTypesAsString = argumentTypes.stream().map(TypeSignature::toString).collect(Collectors.toList());
        return new SimpleSqlOperator(operatorType, typeVariableConstraints, longVariableConstraints, arugmentTypesAsString, returnType, methodHandle, instanceFactory, nullable, nullableArguments, literalParameters);
    }

    protected SqlOperator(OperatorType operatorType, TypeSignature returnType, List<TypeSignature> argumentTypes, Set<String> literalParameters)
    {
        super(mangleOperatorName(operatorType), returnType, argumentTypes, literalParameters);
    }

    protected SqlOperator(OperatorType operatorType, List<TypeVariableConstraint> typeVariableConstraints, List<LongVariableConstraint> longVariableConstraints,  String returnType, List<String> argumentTypes)
    {
        super(mangleOperatorName(operatorType), typeVariableConstraints, longVariableConstraints, returnType, argumentTypes);
    }

    protected SqlOperator(OperatorType operatorType, List<TypeVariableConstraint> typeVariableConstraints, List<LongVariableConstraint> longVariableConstraints, String returnType, List<String> argumentTypes, Set<String> literalParameters)
    {
        super(mangleOperatorName(operatorType), typeVariableConstraints, longVariableConstraints, returnType, argumentTypes, false, literalParameters);
    }

    @Override
    public final boolean isHidden()
    {
        return true;
    }

    @Override
    public final boolean isDeterministic()
    {
        return true;
    }

    @Override
    public final String getDescription()
    {
        // Operators are internal, and don't need a description
        return null;
    }

    public static class SimpleSqlOperator
            extends SqlOperator
    {
        private final MethodHandle methodHandle;
        private final Optional<MethodHandle> instanceFactory;
        private final boolean nullable;
        private final List<Boolean> nullableArguments;

        public SimpleSqlOperator(
                OperatorType operatorType,
                List<TypeVariableConstraint> typeVariableConstraints,
                List<LongVariableConstraint> longVariableConstraints,
                List<String> argumentTypes,
                TypeSignature returnType,
                MethodHandle methodHandle,
                Optional<MethodHandle> instanceFactory,
                boolean nullable,
                List<Boolean> nullableArguments,
                Set<String> literalParameters)
        {
            super(operatorType, typeVariableConstraints, longVariableConstraints, returnType.toString(), argumentTypes, literalParameters);
            this.methodHandle = requireNonNull(methodHandle, "methodHandle is null");
            this.instanceFactory = requireNonNull(instanceFactory, "instanceFactory is null");
            this.nullable = nullable;
            this.nullableArguments = ImmutableList.copyOf(requireNonNull(nullableArguments, "nullableArguments is null"));
        }

        @Override
        public ScalarFunctionImplementation specialize(BoundVariables boundVariables, int arity, TypeManager typeManager, FunctionRegistry functionRegistry)
        {
            return new ScalarFunctionImplementation(nullable, nullableArguments, methodHandle, instanceFactory, isDeterministic());
        }
    }
}
