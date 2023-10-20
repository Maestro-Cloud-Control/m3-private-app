/*
 * Copyright 2023 Maestro Cloud Control LLC
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
 *
 */

package io.maestro3.agent.app.configuration;

import io.maestro3.agent.admin.CheckSignFilter;
import io.maestro3.agent.api.ApiConstants;
import io.maestro3.agent.api.HeadersValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;


@Configuration
@EnableWebSecurity
@Order(1)
public class M3ApiSecurityConfiguration extends WebSecurityConfigurerAdapter {
    private HeadersValidator headersValidator;


    @Autowired
    public M3ApiSecurityConfiguration(@Qualifier(ApiConstants.API_VALIDATOR) HeadersValidator headersValidator) {
        this.headersValidator = headersValidator;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable().formLogin();
        http.antMatcher("/api/admin/**")
                .addFilterBefore(new CheckSignFilter(headersValidator), BasicAuthenticationFilter.class)
                .httpBasic();
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/WEB-INF/pages/**", "/js/**", "/css/**", "/img/**");
    }

}
