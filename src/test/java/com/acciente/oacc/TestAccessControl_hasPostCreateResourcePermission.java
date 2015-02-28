/*
 * Copyright 2009-2015, Acciente LLC
 *
 * Acciente LLC licenses this file to you under the
 * Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.acciente.oacc;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class TestAccessControl_hasPostCreateResourcePermission extends TestAccessControlBase {
   @Test
   public void hasPostCreateResourcePermission_succeedsAsSystemResource() {
      authenticateSystemResource();
      // setup permission without granting it to anything
      final String resourceClassName = generateResourceClass(false, false);
      final String customPermissionName = generateResourceClassPermission(resourceClassName);

      // verify setup
      final Set<ResourceCreatePermission> allResourceCreatePermissionsForResourceClass
            = accessControlContext.getEffectiveResourceCreatePermissions(SYS_RESOURCE, resourceClassName);
      assertThat(allResourceCreatePermissionsForResourceClass.isEmpty(), is(true));

      // verify
      if (!accessControlContext.hasPostCreateResourcePermission(SYS_RESOURCE,
                                                                resourceClassName,
                                                                ResourcePermissions.getInstance(customPermissionName))) {
         fail("checking implicit post-create resource permission should have succeeded for system resource");
      }

      final String domainName = generateDomain();
      final Set<ResourceCreatePermission> allResourceCreatePermissionsForResourceClassAndDomain
            = accessControlContext.getEffectiveResourceCreatePermissions(SYS_RESOURCE, resourceClassName, domainName);
      assertThat(allResourceCreatePermissionsForResourceClassAndDomain.isEmpty(), is(true));

      if (!accessControlContext.hasPostCreateResourcePermission(SYS_RESOURCE,
                                                                resourceClassName,
                                                                ResourcePermissions.getInstance(customPermissionName),
                                                                domainName)) {
         fail("checking implicit post-create resource permission (for a domain) should have succeeded for system resource");
      }
   }

   @Test
   public void hasPostCreateResourcePermission_noPermissions_shouldFailAsAuthenticated() {
      authenticateSystemResource();

      // setup permission without granting it to anything
      final String resourceClassName = generateResourceClass(false, false);
      final String customPermissionName = generateResourceClassPermission(resourceClassName);
      final char[] password = generateUniquePassword();
      final Resource accessorResource = generateAuthenticatableResource(password);

      // verify setup
      final Set<ResourceCreatePermission> allResourceCreatePermissionsForResourceClass
            = accessControlContext.getEffectiveResourceCreatePermissions(accessorResource, resourceClassName);
      assertThat(allResourceCreatePermissionsForResourceClass.isEmpty(), is(true));

      // authenticate accessor/creator resource
      accessControlContext.authenticate(accessorResource, PasswordCredentials.newInstance(password));

      // verify
      if (accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                               resourceClassName,
                                                               ResourcePermissions.getInstance(customPermissionName))) {
         fail("checking post-create resource permission when none has been granted should not have succeeded for authenticated resource");
      }

      final String domainName = generateDomain();
      final Set<ResourceCreatePermission> allResourceCreatePermissionsForResourceClassAndDomain
            = accessControlContext.getEffectiveResourceCreatePermissions(accessorResource, resourceClassName, domainName);
      assertThat(allResourceCreatePermissionsForResourceClassAndDomain.isEmpty(), is(true));
      if (accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                               resourceClassName,
                                                               ResourcePermissions.getInstance(customPermissionName),
                                                               domainName)) {
         fail("checking post-create resource permission for domain when none has been granted should not have succeeded for authenticated resource");
      }
   }

   @Test
   public void hasPostCreateResourcePermission_direct_succeedsAsAuthenticatedResource() {
      authenticateSystemResource();

      final char[] password = generateUniquePassword();
      final Resource accessorResource = generateAuthenticatableResource(password);
      final String accessorDomainName = accessControlContext.getDomainNameByResource(accessorResource);
      final String accessedDomainName = generateDomain();
      final String resourceClassName = generateResourceClass(false, false);

      // setup create permissions
      final String customPermissionName_accessorDomain = generateResourceClassPermission(resourceClassName);
      final ResourcePermission customPermission_forAccessorDomain
            = ResourcePermissions.getInstance(customPermissionName_accessorDomain);
      final ResourceCreatePermission customCreatePermission_accessorDomain_withGrant
            = ResourceCreatePermissions.getInstance(customPermission_forAccessorDomain, true);

      final String customPermissionName_accessedDomain = generateResourceClassPermission(resourceClassName);
      final ResourcePermission customPermission_forAccessedDomain
            = ResourcePermissions.getInstance(customPermissionName_accessedDomain);
      final ResourceCreatePermission customCreatePermission_accessedDomain_withoutGrant
            = ResourceCreatePermissions.getInstance(customPermission_forAccessedDomain, false);

      final ResourceCreatePermission createPermission_withoutGrant
            = ResourceCreatePermissions.getInstance(ResourceCreatePermissions.CREATE, false);
      final ResourceCreatePermission createPermission_withGrant
            = ResourceCreatePermissions.getInstance(ResourceCreatePermissions.CREATE, true);

      grantResourceCreatePermission(accessorResource,
                                    resourceClassName,
                                    accessorDomainName,
                                    createPermission_withGrant,
                                    customCreatePermission_accessorDomain_withGrant);

      grantResourceCreatePermission(accessorResource,
                                    resourceClassName,
                                    accessedDomainName,
                                    createPermission_withoutGrant,
                                    customCreatePermission_accessedDomain_withoutGrant);


      // verify permissions
      final Set<ResourceCreatePermission> allResourceCreatePermissionsForAccessorDomain
            = accessControlContext.getEffectiveResourceCreatePermissions(accessorResource, resourceClassName, accessorDomainName);
      assertThat(allResourceCreatePermissionsForAccessorDomain,
                 is(setOf(createPermission_withGrant, customCreatePermission_accessorDomain_withGrant)));

      final Set<ResourceCreatePermission> allResourceCreatePermissionsForAccessedDomain
            = accessControlContext.getEffectiveResourceCreatePermissions(accessorResource, resourceClassName, accessedDomainName);
      assertThat(allResourceCreatePermissionsForAccessedDomain,
                 is(setOf(createPermission_withoutGrant, customCreatePermission_accessedDomain_withoutGrant)));

      // authenticate accessor/creator resource
      accessControlContext.authenticate(accessorResource, PasswordCredentials.newInstance(password));

      // verify
      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                customPermission_forAccessorDomain)) {
         fail("checking direct post-create resource permission should have succeeded for authenticated resource");
      }

      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                customPermission_forAccessorDomain,
                                                                accessorDomainName)) {
         fail("checking direct post-create resource permission for domain should have succeeded for authenticated resource");
      }

      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                customPermission_forAccessedDomain,
                                                                accessedDomainName)) {
         fail("checking direct post-create resource permission for domain should have succeeded for authenticated resource");
      }
   }

   @Test
   public void hasPostCreateResourcePermission_directWithDifferentGrantingRights_succeedsAsAuthenticatedResource() {
      authenticateSystemResource();

      final char[] password = generateUniquePassword();
      final Resource accessorResource = generateAuthenticatableResource(password);
      final String accessorDomainName = accessControlContext.getDomainNameByResource(accessorResource);
      final String accessedDomainName = generateDomain();
      final String resourceClassName = generateResourceClass(false, false);

      // setup create permissions
      final String customPermissionName_accessorDomain = generateResourceClassPermission(resourceClassName);
      final ResourcePermission grantableCustomPermission_forAccessorDomain
            = ResourcePermissions.getInstance(customPermissionName_accessorDomain, true);
      final ResourcePermission ungrantableCustomPermission_forAccessorDomain
            = ResourcePermissions.getInstance(customPermissionName_accessorDomain, false);
      final ResourceCreatePermission grantableCustomCreatePermission_accessorDomain_withGrant
            = ResourceCreatePermissions.getInstance(grantableCustomPermission_forAccessorDomain, true);

      final String customPermissionName_accessedDomain = generateResourceClassPermission(resourceClassName);
      final ResourcePermission grantableCustomPermission_forAccessedDomain
            = ResourcePermissions.getInstance(customPermissionName_accessedDomain, true);
      final ResourcePermission ungrantableCustomPermission_forAccessedDomain
            = ResourcePermissions.getInstance(customPermissionName_accessedDomain, false);
      final ResourceCreatePermission ungrantableCustomCreatePermission_accessedDomain_withoutGrant
            = ResourceCreatePermissions.getInstance(ungrantableCustomPermission_forAccessedDomain, false);

      final ResourceCreatePermission createPermission_withoutGrant
            = ResourceCreatePermissions.getInstance(ResourceCreatePermissions.CREATE, false);
      final ResourceCreatePermission createPermission_withGrant
            = ResourceCreatePermissions.getInstance(ResourceCreatePermissions.CREATE, true);

      grantResourceCreatePermission(accessorResource,
                                    resourceClassName,
                                    accessorDomainName,
                                    createPermission_withGrant,
                                    grantableCustomCreatePermission_accessorDomain_withGrant);

      grantResourceCreatePermission(accessorResource,
                                    resourceClassName,
                                    accessedDomainName,
                                    createPermission_withoutGrant,
                                    ungrantableCustomCreatePermission_accessedDomain_withoutGrant);


      // verify permissions
      final Set<ResourceCreatePermission> allResourceCreatePermissionsForAccessorDomain
            = accessControlContext.getEffectiveResourceCreatePermissions(accessorResource, resourceClassName, accessorDomainName);
      assertThat(allResourceCreatePermissionsForAccessorDomain,
                 is(setOf(createPermission_withGrant, grantableCustomCreatePermission_accessorDomain_withGrant)));

      final Set<ResourceCreatePermission> allResourceCreatePermissionsForAccessedDomain
            = accessControlContext.getEffectiveResourceCreatePermissions(accessorResource, resourceClassName, accessedDomainName);
      assertThat(allResourceCreatePermissionsForAccessedDomain,
                 is(setOf(createPermission_withoutGrant, ungrantableCustomCreatePermission_accessedDomain_withoutGrant)));

      // authenticate accessor/creator resource
      accessControlContext.authenticate(accessorResource, PasswordCredentials.newInstance(password));

      // verify
      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                grantableCustomPermission_forAccessorDomain)) {
         fail("checking direct post-create resource permission with same granting rights should have succeeded");
      }
      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                ungrantableCustomPermission_forAccessorDomain)) {
         fail("checking direct post-create resource permission with lesser granting rights should have succeeded");
      }

      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                grantableCustomPermission_forAccessorDomain,
                                                                accessorDomainName)) {
         fail("checking direct post-create resource permission with same granting rights (for a domain) should have succeeded");
      }
      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                ungrantableCustomPermission_forAccessorDomain,
                                                                accessorDomainName)) {
         fail("checking direct post-create resource permission with lesser granting rights (for a domain) should have succeeded");
      }

      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                ungrantableCustomPermission_forAccessedDomain,
                                                                accessedDomainName)) {
         fail("checking direct post-create resource permission with same granting rights (for a domain) should have succeeded");
      }
      if (accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                               resourceClassName,
                                                               grantableCustomPermission_forAccessedDomain,
                                                               accessedDomainName)) {
         fail("checking direct post-create resource permission with exceeded granting rights (for a domain) should have failed");
      }
   }

   @Test
   public void hasPostCreateResourcePermission_resourceInherited_succeedsAsAuthenticatedResource() {
      authenticateSystemResource();

      final String resourceClassName = generateResourceClass(false, false);
      final String customPermissionName_forAccessorDomain = generateResourceClassPermission(resourceClassName);
      final String customPermissionName_forAccessedDomain = generateResourceClassPermission(resourceClassName);
      final ResourcePermission customPermission_forAccessorDomain
            = ResourcePermissions.getInstance(customPermissionName_forAccessorDomain);
      final ResourcePermission customPermission_forAccessedDomain
            = ResourcePermissions.getInstance(customPermissionName_forAccessedDomain);
      final char[] password = generateUniquePassword();
      final Resource accessorResource = generateAuthenticatableResource(password);
      final Resource intermediaryResource = generateUnauthenticatableResource();
      final String accessorDomainName = accessControlContext.getDomainNameByResource(accessorResource);
      final String accessedDomainName = generateDomain();

      // setup create permissions
      grantResourceCreatePermission(intermediaryResource, resourceClassName, accessorDomainName, customPermissionName_forAccessorDomain);
      grantResourceCreatePermission(intermediaryResource, resourceClassName, accessedDomainName, customPermissionName_forAccessedDomain);
      // setup inheritance permission
      Set<ResourcePermission> resourcePermissions = new HashSet<>();
      resourcePermissions.add(ResourcePermissions.getInstance(ResourcePermissions.INHERIT));
      accessControlContext.setResourcePermissions(accessorResource, intermediaryResource, resourcePermissions);

      // verify permissions
      Set<ResourceCreatePermission> resourceCreatePermissions_forAccessorDomain = new HashSet<>();
      resourceCreatePermissions_forAccessorDomain.add(ResourceCreatePermissions
                                                            .getInstance(ResourceCreatePermissions.CREATE, false));
      resourceCreatePermissions_forAccessorDomain.add(ResourceCreatePermissions
                                                            .getInstance(customPermission_forAccessorDomain, false));
      final Set<ResourceCreatePermission> allResourceCreatePermissionsForResourceClassAndAccessorDomain
            = accessControlContext.getEffectiveResourceCreatePermissions(intermediaryResource, resourceClassName, accessorDomainName);
      assertThat(allResourceCreatePermissionsForResourceClassAndAccessorDomain, is(resourceCreatePermissions_forAccessorDomain));

      Set<ResourceCreatePermission> resourceCreatePermissions_forAccessedDomain = new HashSet<>();
      resourceCreatePermissions_forAccessedDomain.add(ResourceCreatePermissions
                                                            .getInstance(ResourceCreatePermissions.CREATE, false));
      resourceCreatePermissions_forAccessedDomain.add(ResourceCreatePermissions
                                                            .getInstance(customPermission_forAccessedDomain, false));
      final Set<ResourceCreatePermission> allResourceCreatePermissionsForResourceClassAndAccessedDomain
            = accessControlContext.getEffectiveResourceCreatePermissions(intermediaryResource, resourceClassName, accessedDomainName);
      assertThat(allResourceCreatePermissionsForResourceClassAndAccessedDomain, is(resourceCreatePermissions_forAccessedDomain));

      final Set<ResourcePermission> allResourcePermissionsForAccessorResource
            = accessControlContext.getEffectiveResourcePermissions(accessorResource, intermediaryResource);
      assertThat(allResourcePermissionsForAccessorResource, is(resourcePermissions));

      // authenticate accessor/creator resource
      accessControlContext.authenticate(accessorResource, PasswordCredentials.newInstance(password));

      // verify
      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                customPermission_forAccessorDomain)) {
         fail("checking inherited post-create resource permission should have succeeded");
      }

      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                customPermission_forAccessedDomain,
                                                                accessedDomainName)) {
         fail("checking inherited post-create resource permission for a domain should have succeeded");
      }
   }

   @Test
   public void hasPostCreateResourcePermission_domainInherited_succeedsAsAuthenticatedResource() {
      authenticateSystemResource();

      final String resourceClassName = generateResourceClass(false, false);
      final String customPermissionName_forAccessorDomain = generateResourceClassPermission(resourceClassName);
      final String customPermissionName_forIntermediaryDomain = generateResourceClassPermission(resourceClassName);
      final ResourcePermission customPermission_forAccessorDomain
            = ResourcePermissions.getInstance(customPermissionName_forAccessorDomain);
      final ResourcePermission customPermission_forIntermediaryDomain
            = ResourcePermissions.getInstance(customPermissionName_forIntermediaryDomain);
      final char[] password = generateUniquePassword();
      final Resource accessorResource = generateAuthenticatableResource(password);
      final String accessorDomainName = accessControlContext.getDomainNameByResource(accessorResource);
      final String intermediaryDomainName = generateUniqueDomainName();
      final String accessedDomainName = generateUniqueDomainName();
      accessControlContext.createDomain(intermediaryDomainName, accessorDomainName);
      accessControlContext.createDomain(accessedDomainName, intermediaryDomainName);

      // setup create permissions
      grantResourceCreatePermission(accessorResource, resourceClassName, accessorDomainName, customPermissionName_forAccessorDomain);
      grantResourceCreatePermission(accessorResource, resourceClassName, intermediaryDomainName, customPermissionName_forIntermediaryDomain);

      // verify permissions
      Set<ResourceCreatePermission> resourceCreatePermissions_forAccessorDomain = new HashSet<>();
      resourceCreatePermissions_forAccessorDomain.add(ResourceCreatePermissions
                                                            .getInstance(ResourceCreatePermissions.CREATE, false));
      resourceCreatePermissions_forAccessorDomain.add(ResourceCreatePermissions
                                                            .getInstance(customPermission_forAccessorDomain, false));
      final Set<ResourceCreatePermission> allResourceCreatePermissionsForResourceClassAndAccessorDomain
            = accessControlContext.getEffectiveResourceCreatePermissions(accessorResource, resourceClassName, accessorDomainName);
      assertThat(allResourceCreatePermissionsForResourceClassAndAccessorDomain, is(resourceCreatePermissions_forAccessorDomain));

      Set<ResourceCreatePermission> resourceCreatePermissions_forIntermediaryDomain = new HashSet<>();
      resourceCreatePermissions_forIntermediaryDomain.add(ResourceCreatePermissions
                                                                .getInstance(ResourceCreatePermissions.CREATE, false));
      resourceCreatePermissions_forIntermediaryDomain.add(ResourceCreatePermissions
                                                                .getInstance(customPermission_forIntermediaryDomain, false));
      resourceCreatePermissions_forIntermediaryDomain.addAll(resourceCreatePermissions_forAccessorDomain);
      final Set<ResourceCreatePermission> allResourceCreatePermissionsForResourceClassAndIntermediaryDomain
            = accessControlContext.getEffectiveResourceCreatePermissions(accessorResource, resourceClassName, intermediaryDomainName);
      assertThat(allResourceCreatePermissionsForResourceClassAndIntermediaryDomain, is(resourceCreatePermissions_forIntermediaryDomain));

      // authenticate accessor/creator resource
      accessControlContext.authenticate(accessorResource, PasswordCredentials.newInstance(password));

      // verify
      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                customPermission_forAccessorDomain)) {
         fail("checking domain-inherited post-create resource permission should have succeeded");
      }

      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                customPermission_forAccessorDomain,
                                                                accessedDomainName)) {
         fail("checking domain-inherited post-create resource permission (for accessed domain) should have succeeded");
      }

      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                customPermission_forIntermediaryDomain,
                                                                accessedDomainName)) {
         fail("checking domain-inherited post-create resource permission (for intermediary domain) should have succeeded");
      }
   }

   @Test
   public void hasPostCreateResourcePermission_domainInheritedInherited_succeedsAsAuthenticatedResource() {
      authenticateSystemResource();

      final String resourceClassName = generateResourceClass(false, false);
      final String customPermissionName_forAccessorDomain = generateResourceClassPermission(resourceClassName);
      final String customPermissionName_forIntermediaryDomain = generateResourceClassPermission(resourceClassName);
      final ResourcePermission customPermission_forAccessorDomain
            = ResourcePermissions.getInstance(customPermissionName_forAccessorDomain);
      final ResourcePermission customPermission_forIntermediaryDomain
            = ResourcePermissions.getInstance(customPermissionName_forIntermediaryDomain);
      final char[] password = generateUniquePassword();
      final Resource accessorResource = generateAuthenticatableResource(password);
      final String accessorDomainName = accessControlContext.getDomainNameByResource(accessorResource);
      final Resource donorResource = accessControlContext.createResource(generateResourceClass(false, false), accessorDomainName);
      final String intermediaryDomainName = generateUniqueDomainName();
      final String accessedDomainName = generateUniqueDomainName();
      accessControlContext.createDomain(intermediaryDomainName, accessorDomainName);
      accessControlContext.createDomain(accessedDomainName, intermediaryDomainName);

      // setup create permissions
      grantResourceCreatePermission(donorResource, resourceClassName, accessorDomainName, customPermissionName_forAccessorDomain);
      grantResourceCreatePermission(donorResource, resourceClassName, intermediaryDomainName, customPermissionName_forIntermediaryDomain);
      // setup inheritance permission
      Set<ResourcePermission> inheritanceResourcePermissions = new HashSet<>();
      inheritanceResourcePermissions.add(ResourcePermissions.getInstance(ResourcePermissions.INHERIT));
      accessControlContext.setResourcePermissions(accessorResource, donorResource, inheritanceResourcePermissions);

      // verify permissions
      Set<ResourceCreatePermission> resourceCreatePermissions_forDonorDomain = new HashSet<>();
      resourceCreatePermissions_forDonorDomain.add(ResourceCreatePermissions
                                                         .getInstance(ResourceCreatePermissions.CREATE, false));
      resourceCreatePermissions_forDonorDomain.add(ResourceCreatePermissions
                                                         .getInstance(customPermission_forAccessorDomain, false));
      final Set<ResourceCreatePermission> allResourceCreatePermissionsForResourceClassAndDonorDomain
            = accessControlContext.getEffectiveResourceCreatePermissions(donorResource, resourceClassName, accessorDomainName);
      assertThat(allResourceCreatePermissionsForResourceClassAndDonorDomain, is(resourceCreatePermissions_forDonorDomain));

      Set<ResourceCreatePermission> resourceCreatePermissions_forIntermediaryDomain = new HashSet<>();
      resourceCreatePermissions_forIntermediaryDomain.add(ResourceCreatePermissions
                                                                .getInstance(ResourceCreatePermissions.CREATE, false));
      resourceCreatePermissions_forIntermediaryDomain.add(ResourceCreatePermissions
                                                                .getInstance(customPermission_forIntermediaryDomain, false));
      resourceCreatePermissions_forIntermediaryDomain.addAll(resourceCreatePermissions_forDonorDomain);
      final Set<ResourceCreatePermission> allResourceCreatePermissionsForResourceClassAndIntermediaryDomain
            = accessControlContext.getEffectiveResourceCreatePermissions(donorResource, resourceClassName, intermediaryDomainName);
      assertThat(allResourceCreatePermissionsForResourceClassAndIntermediaryDomain, is(resourceCreatePermissions_forIntermediaryDomain));

      final Set<ResourcePermission> allResourcePermissionsForAccessorResource
            = accessControlContext.getEffectiveResourcePermissions(accessorResource, donorResource);
      assertThat(allResourcePermissionsForAccessorResource, is(inheritanceResourcePermissions));

      // authenticate accessor/creator resource
      accessControlContext.authenticate(accessorResource, PasswordCredentials.newInstance(password));

      // verify
      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                customPermission_forAccessorDomain)) {
         fail("checking inherited domain-inherited post-create resource permission should have succeeded");
      }

      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                customPermission_forAccessorDomain,
                                                                accessedDomainName)) {
         fail("checking inherited domain-inherited post-create resource permission (for accessed domain) should have succeeded");
      }

      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                customPermission_forIntermediaryDomain,
                                                                accessedDomainName)) {
         fail("checking inherited domain-inherited post-create resource permission (for intermediary domain) should have succeeded");
      }
   }

   @Test
   public void hasPostCreateResourcePermission_globalOnly_shouldFailAsAuthenticatedResource() {
      // special case where the requested permission hasn't been granted as a create permission
      // but will be available from the granted global permissions on the {resource class, domain}-tuple
      // Note that in this test case there is no *CREATE permission, and the test should thus fail
      authenticateSystemResource();

      final String resourceClassName = generateResourceClass(false, false);
      final String customPermissionName = generateResourceClassPermission(resourceClassName);
      final ResourcePermission globalResourcePermission = ResourcePermissions.getInstance(customPermissionName);
      final char[] password = generateUniquePassword();
      final Resource accessorResource = generateAuthenticatableResource(password);
      final String accessorDomainName = accessControlContext.getDomainNameByResource(accessorResource);

      // setup global permission
      Set<ResourcePermission> globalResourcePermissions = new HashSet<>();
      globalResourcePermissions.add(globalResourcePermission);
      accessControlContext.setGlobalResourcePermissions(accessorResource,
                                                        resourceClassName,
                                                        globalResourcePermissions,
                                                        accessorDomainName);

      // verify permissions
      final Set<ResourceCreatePermission> allResourceCreatePermissionsForResourceClass
            = accessControlContext.getEffectiveResourceCreatePermissions(accessorResource, resourceClassName, accessorDomainName);
      assertThat(allResourceCreatePermissionsForResourceClass.isEmpty(), is(true));

      final Set<ResourcePermission> allGlobalResourcePermissionsForResourceClass
            = accessControlContext.getEffectiveGlobalResourcePermissions(accessorResource, resourceClassName, accessorDomainName);
      assertThat(allGlobalResourcePermissionsForResourceClass.isEmpty(), is(false));
      assertThat(allGlobalResourcePermissionsForResourceClass, hasItem(globalResourcePermission));

      // authenticate accessor/creator resource
      accessControlContext.authenticate(accessorResource, PasswordCredentials.newInstance(password));

      // verify
      if (accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                               resourceClassName,
                                                               globalResourcePermission)) {
         fail("checking post-create resource permission without *CREATE should not have succeeded for authenticated resource");
      }

      if (accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                               resourceClassName,
                                                               globalResourcePermission,
                                                               accessorDomainName)) {
         fail("checking post-create resource permission without *CREATE should not have succeeded for authenticated resource");
      }
   }

   @Test
   public void hasPostCreateResourcePermission_globalAndDirect_succeedsAsAuthenticatedResource() {
      // special case where some of the requested permission haven't been granted as a create permission
      // but will be available from the granted global permissions on the {resource class, domain}-tuple
      authenticateSystemResource();

      final String resourceClassName = generateResourceClass(true, false);
      final String globalPermissionName = generateResourceClassPermission(resourceClassName);
      final String customPermissionName = generateResourceClassPermission(resourceClassName);
      final ResourcePermission globalResourcePermission = ResourcePermissions.getInstance(globalPermissionName);
      final ResourcePermission customResourcePermission = ResourcePermissions.getInstance(customPermissionName);
      final ResourcePermission systemResourcePermission = ResourcePermissions.getInstance(ResourcePermissions.RESET_CREDENTIALS);
      final char[] password = generateUniquePassword();
      final Resource accessorResource = generateAuthenticatableResource(password);
      final String accessorDomainName = accessControlContext.getDomainNameByResource(accessorResource);

      // setup direct resource create permissions
      Set<ResourceCreatePermission> resourceCreatePermissions = new HashSet<>();
      final ResourceCreatePermission createPermission_create
            = ResourceCreatePermissions.getInstance(ResourceCreatePermissions.CREATE, false);
      final ResourceCreatePermission createPermission_custom
            = ResourceCreatePermissions.getInstance(customResourcePermission, false);
      final ResourceCreatePermission createPermission_system
            = ResourceCreatePermissions.getInstance(systemResourcePermission, false);
      resourceCreatePermissions.add(createPermission_create);
      resourceCreatePermissions.add(createPermission_custom);
      resourceCreatePermissions.add(createPermission_system);
      accessControlContext.setResourceCreatePermissions(accessorResource,
                                                        resourceClassName,
                                                        resourceCreatePermissions,
                                                        accessorDomainName);
      // setup global permission
      Set<ResourcePermission> globalResourcePermissions = new HashSet<>();
      globalResourcePermissions.add(globalResourcePermission);
      accessControlContext.setGlobalResourcePermissions(accessorResource,
                                                        resourceClassName,
                                                        globalResourcePermissions,
                                                        accessorDomainName);

      // verify permissions
      final Set<ResourceCreatePermission> allResourceCreatePermissionsForResourceClass
            = accessControlContext.getEffectiveResourceCreatePermissions(accessorResource, resourceClassName, accessorDomainName);
      assertThat(allResourceCreatePermissionsForResourceClass.isEmpty(), is(false));
      assertThat(allResourceCreatePermissionsForResourceClass.size(), is(3));
      assertThat(allResourceCreatePermissionsForResourceClass,
                 hasItems(createPermission_create, createPermission_custom, createPermission_system));

      final Set<ResourcePermission> allGlobalResourcePermissionsForResourceClass
            = accessControlContext.getEffectiveGlobalResourcePermissions(accessorResource, resourceClassName, accessorDomainName);
      assertThat(allGlobalResourcePermissionsForResourceClass.isEmpty(), is(false));
      assertThat(allGlobalResourcePermissionsForResourceClass, hasItem(globalResourcePermission));

      // authenticate accessor/creator resource
      accessControlContext.authenticate(accessorResource, PasswordCredentials.newInstance(password));

      // verify
      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                globalResourcePermission)) {
         fail("checking global permission as post-create resource permission should have succeeded");
      }
      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                customResourcePermission)) {
         fail("checking global permission and custom post-create resource permission should have succeeded");
      }
      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                systemResourcePermission)) {
         fail("checking global permission and system post-create resource permission should have succeeded");
      }

      // verify by domain
      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                globalResourcePermission,
                                                                accessorDomainName)) {
         fail("checking global permission as post-create resource permission for a domain should have succeeded");
      }

      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                customResourcePermission,
                                                                accessorDomainName)) {
         fail("checking global permission and custom post-create resource permission for a domain should have succeeded");
      }

      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                systemResourcePermission,
                                                                accessorDomainName)) {
         fail("checking global permission and system post-create resource permission for a domain should have succeeded");
      }
   }

   @Test
   public void hasPostCreateResourcePermission_globalWithDifferentGrantingRights_succeedsAsAuthenticatedResource() {
      authenticateSystemResource();

      final char[] password = generateUniquePassword();
      final Resource accessorResource = generateAuthenticatableResource(password);
      final String accessorDomainName = accessControlContext.getDomainNameByResource(accessorResource);
      final String accessedDomainName = generateDomain();
      final String resourceClassName = generateResourceClass(false, false);

      // setup create permissions
      final ResourceCreatePermission createPermission_withoutGrant
            = ResourceCreatePermissions.getInstance(ResourceCreatePermissions.CREATE, false);
      final ResourceCreatePermission createPermission_withGrant
            = ResourceCreatePermissions.getInstance(ResourceCreatePermissions.CREATE, true);

      accessControlContext.setResourceCreatePermissions(accessorResource,
                                                        resourceClassName,
                                                        setOf(createPermission_withGrant),
                                                        accessorDomainName);
      accessControlContext.setResourceCreatePermissions(accessorResource,
                                                        resourceClassName,
                                                        setOf(createPermission_withoutGrant),
                                                        accessedDomainName);

      // setup global permission
      final String globalPermissionName1 = generateResourceClassPermission(resourceClassName);
      final ResourcePermission grantableGlobalPermission1 = ResourcePermissions.getInstance(globalPermissionName1, true);
      final ResourcePermission ungrantableGlobalPermission1 = ResourcePermissions.getInstance(globalPermissionName1);
      final String globalPermissionName2 = generateResourceClassPermission(resourceClassName);
      final ResourcePermission grantableGlobalPermission2 = ResourcePermissions.getInstance(globalPermissionName2, true);
      final ResourcePermission ungrantableGlobalPermission2 = ResourcePermissions.getInstance(globalPermissionName2);
      accessControlContext.setGlobalResourcePermissions(accessorResource,
                                                        resourceClassName,
                                                        setOf(grantableGlobalPermission1),
                                                        accessorDomainName);

      accessControlContext.setGlobalResourcePermissions(accessorResource,
                                                        resourceClassName,
                                                        setOf(ungrantableGlobalPermission2),
                                                        accessedDomainName);

      // authenticate accessor/creator resource
      accessControlContext.authenticate(accessorResource, PasswordCredentials.newInstance(password));

      // verify
      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                grantableGlobalPermission1)) {
         fail("checking post-create resource permission for a global permission with same granting rights should have succeeded");
      }
      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                ungrantableGlobalPermission1)) {
         fail("checking post-create resource permission for a global permission with lesser granting rights should have succeeded");
      }

      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                grantableGlobalPermission1,
                                                                accessorDomainName)) {
         fail("checking post-create resource permission for a global permission with same granting rights (for a domain) should have succeeded");
      }

      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                ungrantableGlobalPermission1,
                                                                accessorDomainName)) {
         fail("checking post-create resource permission for a global permission with lesser granting rights (for a domain) should have succeeded");
      }

      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                ungrantableGlobalPermission2,
                                                                accessedDomainName)) {
         fail("checking post-create resource permission for a global permission with same granting rights (for a domain) should have succeeded");
      }

      if (accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                               resourceClassName,
                                                               grantableGlobalPermission2,
                                                               accessedDomainName)) {
         fail("checking post-create resource permission for a global (create) permission (for a domain) with exceeded granting rights should have failed");
      }
   }

   @Test
   public void hasPostCreateResourcePermission_superUser_succeedsAsAuthenticatedResource() {
      authenticateSystemResource();

      final String resourceClassName = generateResourceClass(false, false);
      final String customPermissionName = generateResourceClassPermission(resourceClassName);
      final ResourcePermission globalResourcePermission = ResourcePermissions.getInstance(customPermissionName);
      final char[] password = generateUniquePassword();
      final Resource accessorResource = generateAuthenticatableResource(password);
      final String accessorDomainName = accessControlContext.getDomainNameByResource(accessorResource);

      // setup super-user domain permission
      accessControlContext.setDomainPermissions(accessorResource,
                                                accessorDomainName,
                                                setOf(DomainPermissions.getInstance(DomainPermissions.SUPER_USER)));

      // authenticate accessor/creator resource
      accessControlContext.authenticate(accessorResource, PasswordCredentials.newInstance(password));

      // verify
      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                globalResourcePermission)) {
         fail("checking implicit post-create resource permission when having super-user privileges should have succeeded for authenticated resource");
      }

      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                globalResourcePermission,
                                                                accessorDomainName)) {
         fail("checking implicit post-create resource permission (for a domain) when having super-user privileges should have succeeded for authenticated resource");
      }
   }

   @Test
   public void hasPostCreateResourcePermission_superUserInherited_succeedsAsAuthenticatedResource() {
      authenticateSystemResource();

      final String resourceClassName = generateResourceClass(false, false);
      final String customPermissionName = generateResourceClassPermission(resourceClassName);
      final ResourcePermission globalResourcePermission = ResourcePermissions.getInstance(customPermissionName);
      final char[] password = generateUniquePassword();
      final Resource accessorResource = generateAuthenticatableResource(password);
      final String accessorDomainName = accessControlContext.getDomainNameByResource(accessorResource);

      // setup super-user domain permission
      final Resource donorResource = generateUnauthenticatableResource();
      accessControlContext.setDomainPermissions(donorResource,
                                                accessorDomainName,
                                                setOf(DomainPermissions.getInstance(DomainPermissions.SUPER_USER)));

      // setup accessor --INHERIT-> donor
      accessControlContext.setResourcePermissions(accessorResource,
                                                  donorResource,
                                                  setOf(ResourcePermissions.getInstance(ResourcePermissions.INHERIT)));

      // authenticate accessor/creator resource
      accessControlContext.authenticate(accessorResource, PasswordCredentials.newInstance(password));

      // verify
      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                globalResourcePermission)) {
         fail("checking implicit post-create resource permission when inheriting super-user privileges should have succeeded for authenticated resource");
      }

      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName,
                                                                globalResourcePermission,
                                                                accessorDomainName)) {
         fail("checking implicit post-create resource permission (for a domain) when inheriting super-user privileges should have succeeded for authenticated resource");
      }
   }

   @Test
   public void hasPostCreateResourcePermission_superUserInvalidPermission_shouldFailAsSystemResource() {
      authenticateSystemResource();
      // setup resourceClass without any permissions
      final String resourceClassName = generateResourceClass(false, false);

      // verify setup
      final Set<ResourceCreatePermission> allResourceCreatePermissionsForResourceClass
            = accessControlContext.getEffectiveResourceCreatePermissions(SYS_RESOURCE, resourceClassName);
      assertThat(allResourceCreatePermissionsForResourceClass.isEmpty(), is(true));

      // verify
      try {
         accessControlContext
               .hasPostCreateResourcePermission(SYS_RESOURCE,
                                                resourceClassName,
                                                ResourcePermissions.getInstance(ResourcePermissions.RESET_CREDENTIALS));
         fail("checking implicit resource create permission invalid for resource class should have failed for system resource");
      }
      catch (IllegalArgumentException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("not valid for unauthenticatable resource class"));
      }
      try {
         accessControlContext
               .hasPostCreateResourcePermission(SYS_RESOURCE,
                                                resourceClassName,
                                                ResourcePermissions.getInstance(ResourcePermissions.IMPERSONATE));
         fail("checking implicit resource create permission invalid for resource class should have failed for system resource");
      }
      catch (IllegalArgumentException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("not valid for unauthenticatable resource class"));
      }

      final String domainName = generateDomain();
      try {
         accessControlContext
               .hasPostCreateResourcePermission(SYS_RESOURCE,
                                                resourceClassName,
                                                ResourcePermissions.getInstance(ResourcePermissions.RESET_CREDENTIALS),
                                                domainName);
         fail("checking implicit resource create permission (for a domain) invalid for resource class should have failed for system resource");
      }
      catch (IllegalArgumentException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("not valid for unauthenticatable resource class"));
      }
      try {
         accessControlContext
               .hasPostCreateResourcePermission(SYS_RESOURCE,
                                                resourceClassName,
                                                ResourcePermissions.getInstance(ResourcePermissions.IMPERSONATE),
                                                domainName);
         fail("checking implicit resource create permission (for a domain) invalid for resource class should have failed for system resource");
      }
      catch (IllegalArgumentException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("not valid for unauthenticatable resource class"));
      }
   }

   @Test
   public void hasPostCreateResourcePermission_whitespaceConsistent() {
      authenticateSystemResource();
      // setup permission without granting it to anything
      final String resourceClassName = generateResourceClass(false, false);
      final String resourceClassName_whitespaced = " " + resourceClassName + "\t";
      final String customPermissionName = generateResourceClassPermission(resourceClassName);

      // verify setup
      final Set<ResourceCreatePermission> allResourceCreatePermissionsForResourceClass
            = accessControlContext.getEffectiveResourceCreatePermissions(SYS_RESOURCE, resourceClassName);
      assertThat(allResourceCreatePermissionsForResourceClass.isEmpty(), is(true));

      // verify

      // checking post-create resource permission (even when none has been granted) should succeed for system resource
      if (!accessControlContext.hasPostCreateResourcePermission(SYS_RESOURCE,
                                                                resourceClassName_whitespaced,
                                                                ResourcePermissions.getInstance(customPermissionName))) {
         fail("checking post-create resource permission on whitespaced resource class name should have succeeded for system resource");
      }

      final String domainName = generateDomain();
      final String domainName_whitespaced = " " + domainName + "\t";
      final Set<ResourceCreatePermission> allResourceCreatePermissionsForResourceClassAndDomain
            = accessControlContext.getEffectiveResourceCreatePermissions(SYS_RESOURCE, resourceClassName, domainName);
      assertThat(allResourceCreatePermissionsForResourceClassAndDomain.isEmpty(), is(true));

      if (!accessControlContext.hasPostCreateResourcePermission(SYS_RESOURCE,
                                                                resourceClassName_whitespaced,
                                                                ResourcePermissions.getInstance(customPermissionName),
                                                                domainName_whitespaced)) {
         fail("checking post-create resource permission on whitespaced resource class and domain name should have succeeded for system resource");
      }
   }

   @Test
   public void hasPostCreateResourcePermission_whitespaceConsistent_asAuthenticatedResource() {
      authenticateSystemResource();

      final String resourceClassName = generateResourceClass(false, false);
      final String resourceClassName_whitespaced = " " + resourceClassName + "\t";
      final String customPermissionName_forAccessorDomain = generateResourceClassPermission(resourceClassName);
      final String customPermissionName_forAccessedDomain = generateResourceClassPermission(resourceClassName);
      final ResourcePermission customPermission_forAccessorDomain = ResourcePermissions.getInstance(
            customPermissionName_forAccessorDomain);
      final ResourcePermission customPermission_forAccessedDomain = ResourcePermissions.getInstance(
            customPermissionName_forAccessedDomain);
      final char[] password = generateUniquePassword();
      final Resource accessorResource = generateAuthenticatableResource(password);
      final String accessorDomainName = accessControlContext.getDomainNameByResource(accessorResource);
      final String accessedDomainName = generateDomain();
      final String accessedDomainName_whitespaced = " " + accessedDomainName + "\t";

      // setup create permissions
      grantResourceCreatePermission(accessorResource, resourceClassName, accessorDomainName, customPermissionName_forAccessorDomain);
      grantResourceCreatePermission(accessorResource, resourceClassName, accessedDomainName, customPermissionName_forAccessedDomain);

      // authenticate accessor/creator resource
      accessControlContext.authenticate(accessorResource, PasswordCredentials.newInstance(password));

      // verify
      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName_whitespaced,
                                                                customPermission_forAccessorDomain)) {
         fail("checking post-create resource permission on whitespaced resource class name should have succeeded for authenticated resource");
      }

      if (!accessControlContext.hasPostCreateResourcePermission(accessorResource,
                                                                resourceClassName_whitespaced,
                                                                customPermission_forAccessedDomain,
                                                                accessedDomainName_whitespaced)) {
         fail("checking post-create resource permission on whitespaced resource class and domain name should have succeeded for authenticated resource");
      }
   }


   @Test
   public void hasPostCreateResourcePermission_nulls_shouldFail() {
      authenticateSystemResource();

      final String resourceClassName = generateResourceClass(false, false);
      final String customPermissionName = generateResourceClassPermission(resourceClassName);

      try {
         accessControlContext.hasPostCreateResourcePermission(null,
                                                              resourceClassName,
                                                              ResourcePermissions.getInstance(customPermissionName));
         fail("checking post-create resource permission for null accessor resource reference should have failed for system resource");
      }
      catch (NullPointerException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("resource required"));
      }
      try {
         accessControlContext.hasPostCreateResourcePermission(SYS_RESOURCE,
                                                              null,
                                                              ResourcePermissions.getInstance(customPermissionName));
         fail("checking post-create resource permission for null resource class reference should have failed for system resource");
      }
      catch (NullPointerException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("resource class required"));
      }
      try {
         accessControlContext.hasPostCreateResourcePermission(SYS_RESOURCE,
                                                              resourceClassName,
                                                              null);
         fail("checking post-create resource permission for null resource permission reference should have failed for system resource");
      }
      catch (NullPointerException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("resource permission required"));
      }

      final String domainName = generateDomain();
      try {
         accessControlContext.hasPostCreateResourcePermission(null,
                                                              resourceClassName,
                                                              ResourcePermissions.getInstance(customPermissionName),
                                                              domainName);
         fail("checking post-create resource permission (by domain) for null accessor resource reference should have failed for system resource");
      }
      catch (NullPointerException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("resource required"));
      }
      try {
         accessControlContext.hasPostCreateResourcePermission(SYS_RESOURCE,
                                                              null,
                                                              ResourcePermissions.getInstance(customPermissionName),
                                                              domainName);
         fail("checking post-create resource permission (by domain) for null resource class reference should have failed for system resource");
      }
      catch (NullPointerException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("resource class required"));
      }
      try {
         accessControlContext.hasPostCreateResourcePermission(SYS_RESOURCE,
                                                              resourceClassName,
                                                              null,
                                                              domainName);
         fail("checking post-create resource permission (by domain) for null resource permission reference should have failed for system resource");
      }
      catch (NullPointerException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("resource permission required"));
      }
      try {
         accessControlContext.hasPostCreateResourcePermission(SYS_RESOURCE,
                                                              resourceClassName,
                                                              ResourcePermissions.getInstance(customPermissionName),
                                                              null);
         fail("checking post-create resource permission (by domain) for null domain reference should have failed for system resource");
      }
      catch (NullPointerException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("domain required"));
      }
   }

   @Test
   public void hasPostCreateResourcePermission_nonExistentReferences_shouldSucceed() {
      // these checks "succeed" in the sense that the has-permission method
      // returns false, as opposed to throwing an exception
      authenticateSystemResource();

      final String resourceClassName = generateResourceClass(false, false);
      final String customPermissionName = generateResourceClassPermission(resourceClassName);

      if (accessControlContext.hasPostCreateResourcePermission(Resources.getInstance(-999L),
                                                               resourceClassName,
                                                               ResourcePermissions.getInstance(customPermissionName))) {
         fail("checking post-create resource permission for invalid accessor resource reference should have failed for system resource");
      }

      final String domainName = generateDomain();
      if (accessControlContext.hasPostCreateResourcePermission(Resources.getInstance(-999L),
                                                               resourceClassName,
                                                               ResourcePermissions.getInstance(customPermissionName),
                                                               domainName)) {
         fail("checking post-create resource permission (by domain) for invalid accessor resource reference should have failed for system resource");
      }
   }

   @Test
   public void hasPostCreateResourcePermission_nonExistentReferences_shouldFail() {
      authenticateSystemResource();

      final String resourceClassName = generateResourceClass(false, false);
      final String customPermissionName = generateResourceClassPermission(resourceClassName);

      try {
         accessControlContext.hasPostCreateResourcePermission(SYS_RESOURCE,
                                                              "invalid_resource_class",
                                                              ResourcePermissions.getInstance(customPermissionName));
         fail("checking post-create resource permission for invalid resource class reference should have failed for system resource");
      }
      catch (IllegalArgumentException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("not defined for resource class"));
      }
      try {
         accessControlContext.hasPostCreateResourcePermission(SYS_RESOURCE,
                                                              resourceClassName,
                                                              ResourcePermissions.getInstance("invalid_permission"));
         fail("checking post-create resource permission for invalid resource permission reference should have failed for system resource");
      }
      catch (IllegalArgumentException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("not defined for resource class"));
      }

      final String domainName = generateDomain();
      try {
         accessControlContext.hasPostCreateResourcePermission(SYS_RESOURCE,
                                                              "invalid_resource_class",
                                                              ResourcePermissions.getInstance(customPermissionName),
                                                              domainName);
         fail("checking post-create resource permission (by domain) for invalid resource class reference should have failed for system resource");
      }
      catch (IllegalArgumentException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("not defined for resource class"));
      }
      try {
         accessControlContext.hasPostCreateResourcePermission(SYS_RESOURCE,
                                                              resourceClassName,
                                                              ResourcePermissions.getInstance("invalid_permission"),
                                                              domainName);
         fail("checking post-create resource permission (by domain) for invalid resource permission reference should have failed for system resource");
      }
      catch (IllegalArgumentException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("not defined for resource class"));
      }
      try {
         accessControlContext.hasPostCreateResourcePermission(SYS_RESOURCE,
                                                              resourceClassName,
                                                              ResourcePermissions.getInstance(customPermissionName),
                                                              "invalid_domain");
         fail("checking post-create resource permission (by domain) for invalid domain reference should have failed for system resource");
      }
      catch (IllegalArgumentException e) {
         assertThat(e.getMessage().toLowerCase(), containsString("could not find domain"));
      }
   }
}
