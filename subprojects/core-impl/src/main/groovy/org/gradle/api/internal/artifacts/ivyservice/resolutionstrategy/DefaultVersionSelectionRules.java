/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.artifacts.ivyservice.resolutionstrategy;

import com.google.common.collect.Lists;
import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.InvalidUserCodeException;
import org.gradle.api.artifacts.*;
import org.gradle.api.artifacts.ivy.IvyModuleDescriptor;
import org.gradle.api.internal.UserCodeAction;
import org.gradle.api.internal.artifacts.VersionSelectionInternal;
import org.gradle.api.internal.artifacts.VersionSelectionRulesInternal;
import org.gradle.api.internal.artifacts.ivyservice.DefaultIvyModuleDescriptor;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.BuildableModuleVersionMetaDataResolveResult;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.DefaultBuildableModuleVersionMetaDataResolveResult;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ModuleComponentRepositoryAccess;
import org.gradle.api.internal.artifacts.metadata.ComponentMetadataAdapter;
import org.gradle.api.internal.artifacts.metadata.IvyModuleVersionMetaData;
import org.gradle.api.internal.artifacts.repositories.resolver.ComponentMetadataDetailsAdapter;

import java.util.*;

public class DefaultVersionSelectionRules implements VersionSelectionRulesInternal {
    final Set<Action<? super VersionSelection>> versionSelectionActions = new LinkedHashSet<Action<? super VersionSelection>>();
    final Set<MetadataRule<? super VersionSelection>> versionSelectionRules = new LinkedHashSet<MetadataRule<? super VersionSelection>>();

    private final static List<Class<?>> VALID_INPUT_TYPES = Lists.newArrayList(ComponentMetadata.class, IvyModuleDescriptor.class);
    private final static String USER_CODE_ERROR = "Could not apply version selection rule with all().";

    public void apply(VersionSelection selection, ModuleComponentRepositoryAccess moduleAccess) {
        for (Action<? super VersionSelection> action : versionSelectionActions) {
            new UserCodeAction<VersionSelection>(USER_CODE_ERROR, action).execute(selection);
        }

        for (MetadataRule<? super VersionSelection> rule: versionSelectionRules) {
            if (rule.getInputTypes() == null || rule.getInputTypes().size() == 0) {
                try {
                    rule.execute(selection, null);
                } catch (Exception e) {
                    throw new InvalidUserCodeException(USER_CODE_ERROR, e);
                }
            } else {
                List<Object> inputs = Lists.newArrayList();
                for (Class<?> inputType : rule.getInputTypes()) {
                    BuildableModuleVersionMetaDataResolveResult descriptorResult = new DefaultBuildableModuleVersionMetaDataResolveResult();
                    moduleAccess.resolveComponentMetaData(((VersionSelectionInternal)selection).getDependencyMetaData(), selection.getCandidate(), descriptorResult);
                    if (inputType == ComponentMetadata.class) {
                        ComponentMetadataDetails details = new ComponentMetadataDetailsAdapter(descriptorResult.getMetaData());
                        ComponentMetadata componentMetadata = new ComponentMetadataAdapter(details);
                        inputs.add(componentMetadata);
                        continue;
                    }
                    if (inputType == IvyModuleDescriptor.class) {
                        if (descriptorResult.getMetaData() instanceof IvyModuleVersionMetaData) {
                            IvyModuleVersionMetaData ivyMetadata = (IvyModuleVersionMetaData) descriptorResult.getMetaData();
                            inputs.add(new DefaultIvyModuleDescriptor(ivyMetadata.getExtraInfo(), ivyMetadata.getBranch(), ivyMetadata.getStatus()));
                            continue;
                        } else {
                            return;
                        }
                    }
                    throw new InvalidUserCodeException(String.format("Unsupported parameter type for version selection rule: %s", inputType.getName()));
                }

                try {
                    rule.execute(selection, inputs);
                } catch (Exception e) {
                    throw new InvalidUserCodeException(USER_CODE_ERROR, e);
                }
            }
        }
    }

    public boolean hasRules() {
        return versionSelectionRules.size() + versionSelectionActions.size() > 0;
    }

    public VersionSelectionRules all(final Action<? super VersionSelection> selectionAction) {
        versionSelectionActions.add(selectionAction);
        return this;
    }

    public VersionSelectionRules all(MetadataRule<? super VersionSelection> metadataRule) {
        versionSelectionRules.add(metadataRule);
        return this;
    }

    public VersionSelectionRules all(Closure<?> closure) {
        Class<?>[] parameterTypes = closure.getParameterTypes();

        if (parameterTypes.length == 0) {
            throw new InvalidUserCodeException(
                    String.format("First parameter of a version selection rule needs to be of type '%s'.",
                            VersionSelection.class.getSimpleName()));
        } else {
            List<Class<?>> inputTypes = Lists.newArrayList();

            if (parameterTypes[0] != VersionSelection.class) {
                throw new InvalidUserCodeException(
                        String.format("First parameter of a version selection rule needs to be of type '%s'.",
                                VersionSelection.class.getSimpleName()));
            }

            for (Class<?> parameterType : Arrays.asList(parameterTypes).subList(1, parameterTypes.length)) {
                if (VALID_INPUT_TYPES.contains(parameterType)) {
                    inputTypes.add(parameterType);
                } else {
                    throw new InvalidUserCodeException(String.format("Unsupported parameter type for version selection rule: %s", parameterType.getName()));
                }
            }

            versionSelectionRules.add(new VersionSelectionRule(closure, inputTypes));
        }

        return this;
    }

    private class VersionSelectionRule implements MetadataRule<VersionSelection> {
        Closure<?> closure;
        List<Class<?>> inputTypes;

        public VersionSelectionRule(Closure<?> closure, List<Class<?>> inputTypes) {
            this.closure = closure;
            this.inputTypes = inputTypes;
        }

        public Class<VersionSelection> getSubjectType() {
            return VersionSelection.class;
        }

        public List<Class<?>> getInputTypes() {
            return inputTypes;
        }

        public void execute(VersionSelection subject, List<?> inputs) {
            if (inputs == null || inputs.size() == 0) {
                closure.call(subject);
            } else {
                Object[] argList = new Object[inputs.size()+1];
                argList[0] = subject;
                int i = 1;
                for (Object arg : inputs) {
                    argList[i++] = arg;
                }
                closure.call(argList);
            }

        }
    }


}
