/*
 * Copyright 2010, Red Hat, Inc. and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.zanata.rest.compat.v1_4_4;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;

import org.dbunit.operation.DatabaseOperation;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.seam.mock.EnhancedMockHttpServletRequest;
import org.jboss.seam.mock.EnhancedMockHttpServletResponse;
import org.jboss.seam.mock.ResourceRequestEnvironment.Method;
import org.jboss.seam.mock.ResourceRequestEnvironment.ResourceRequest;
import org.testng.annotations.Test;
import org.zanata.ZanataCompatibilityTest;
import org.zanata.v1_4_4.rest.MediaTypes;
import org.zanata.v1_4_4.rest.client.IAccountResource;
import org.zanata.v1_4_4.rest.dto.Account;

public class AccountRawCompatibilityTest extends ZanataCompatibilityTest
{

   @Override
   protected void prepareDBUnitOperations()
   {
      beforeTestOperations.add(new DataSetOperation("org/zanata/test/model/AccountData.dbunit.xml", DatabaseOperation.CLEAN_INSERT));
   }
   
   @Test
   public void getAccountJson() throws Exception
   {
      // No client method for Json Get, so testing raw compatibility
      new ResourceRequest(unauthorizedEnvironment, Method.GET, "/restv1/accounts/u/demo")
      {
         @Override
         protected void prepareRequest(EnhancedMockHttpServletRequest request)
         {
            request.addHeader(HttpHeaders.ACCEPT, MediaTypes.APPLICATION_ZANATA_ACCOUNT_JSON);
         }

         @Override
         protected void onResponse(EnhancedMockHttpServletResponse response)
         {
            assertThat(response.getStatus(), is(200)); // Ok
            assertJsonUnmarshal(response, Account.class);
            Account account = jsonUnmarshal(response, Account.class);
            
            // Assert correct parsing of all properties
            assertThat(account.getUsername(), is("demo"));
            assertThat(account.getApiKey(), is("23456789012345678901234567890123"));
            assertThat(account.getEmail(), is("user1@localhost"));
            assertThat(account.getName(), is("Sample User"));
            assertThat(account.getPasswordHash(), is("/9Se/pfHeUH8FJ4asBD6jQ=="));
            assertThat(account.getRoles().size(), is(1));
            //assertThat(account.getTribes().size(), is(1)); // Language teams are not being returned
         }
      }.run();
   }
   
   @Test
   public void putAccountJson() throws Exception
   {
      // New Account
      Account a = new Account("aacount2@localhost.com", "Sample Account", "sampleaccount", "/9Se/pfHeUH8FJ4asBD6jQ==");
      
      UnimplementedIAccountResource accountClient = super.createProxy(UnimplementedIAccountResource.class, "/accounts/u/sampleaccount");
      IAccountResource originalAccountClient = super.createProxy(IAccountResource.class, "/accounts/u/sampleaccount");
      ClientResponse putResponse = accountClient.putJson( a );
      
      // Assert initial put
      assertThat(putResponse.getStatus(), is(Status.CREATED.getStatusCode()));
      
      // Modified Account
      a.setName("New Account Name");
      putResponse = accountClient.putJson( a );
      
      // Assert modification
      assertThat(putResponse.getStatus(), is(Status.OK.getStatusCode()));
      
      // Retrieve again
      Account a2 = originalAccountClient.get().getEntity();
      assertThat(a2.getUsername(), is(a.getUsername()));
      assertThat(a2.getApiKey(), is(a.getApiKey()));
      assertThat(a2.getEmail(), is(a.getEmail()));
      assertThat(a2.getName(), is(a.getName()));
      assertThat(a2.getPasswordHash(), is(a.getPasswordHash()));
      assertThat(a2.getRoles().size(), is(0));
      //assertThat(a2.getTribes().size(), is(1)); // Language teams are not being returned
   }
   
   /**
    * Test interface for any unimplemented rest methods in the original client interface.
    * Helps with the testing of methods that are not present in the original interface to test.
    */
   public static interface UnimplementedIAccountResource
   {
      @PUT
      @Consumes(
      {MediaTypes.APPLICATION_ZANATA_ACCOUNT_JSON})
      public ClientResponse putJson(Account account);
   }

}
