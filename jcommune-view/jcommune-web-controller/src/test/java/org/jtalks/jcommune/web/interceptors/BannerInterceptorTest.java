/**
 * Copyright (C) 2011  JTalks.org Team
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jtalks.jcommune.web.interceptors;

import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.web.ModelAndViewAssert.assertModelAttributeAvailable;

import org.jtalks.jcommune.service.BannerService;
import org.mockito.Mock;

import org.springframework.web.servlet.ModelAndView;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * 
 * @author Anuar_Nurmakanov
 *
 */
public class BannerInterceptorTest {
    @Mock
    private BannerService bannerService;
    
    private BannerInterceptor bannerInterceptor;
    
    @BeforeMethod
    public void init() {
        initMocks(this);
        bannerInterceptor = new BannerInterceptor(bannerService);
    }
    
    @Test
    public void postHandleShouldAddDataForBanners() throws Exception {
        ModelAndView modelAndView = new ModelAndView("a view");
        
        bannerInterceptor.postHandle(null, null, null, modelAndView);
        
        assertModelAttributeAvailable(modelAndView, BannerInterceptor.BANNERS_MODEL_PARAM);
        assertModelAttributeAvailable(modelAndView, BannerInterceptor.UPLOADED_BANNER_MODEL_PARAM);
    }
    
    @Test
    public void postHandleShouldNotAddDataForBannersWhenModelIsNull() throws Exception {
        bannerInterceptor.postHandle(null, null, null, null);
    }
}
