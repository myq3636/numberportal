package com.liugh.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.liugh.entity.AdminInfo;
import com.liugh.entity.RcsFile;
import com.liugh.rcs.service.RcsCustomerAccessService;
import com.liugh.rcs.service.RcsFileService;

public class RcsFileControllerTest {

    private RcsCustomerAccessService accessService;
    private RcsFileService fileService;
    private RcsFileResourceController controller;

    @Before
    public void setUp() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest(), new MockHttpServletResponse()));
        accessService = mock(RcsCustomerAccessService.class);
        fileService = mock(RcsFileService.class);
        controller = new RcsFileResourceController(accessService, fileService);
        AdminInfo customer = new AdminInfo();
        customer.setSsid(100);
        when(accessService.requireCustomer(any(com.alibaba.fastjson.JSONObject.class)))
                .thenReturn(customer);
    }

    @After
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void returnsOwnedFileMetadata() {
        RcsFile file = new RcsFile();
        file.setSsid(100);
        file.setUid("file_1");
        file.setFileName("photo.jpg");
        file.setMimeType("image/jpeg");
        when(fileService.getOwned(100, "file_1")).thenReturn(file);

        assertEquals("photo.jpg",
                controller.get("file_1", "customer-token", null)
                        .getData().getString("fileName"));
    }

    @Test
    public void deletesOwnedFileWithQueryToken() throws Exception {
        controller.delete("file_1", null, "customer-token");

        verify(fileService).deleteOwned(100, "file_1");
    }
}
