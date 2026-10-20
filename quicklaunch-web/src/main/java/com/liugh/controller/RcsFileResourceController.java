package com.liugh.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.liugh.annotation.Pass;
import com.liugh.config.ResponseHelper;
import com.liugh.config.ResponseModel;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.RcsFile;
import com.liugh.rcs.service.RcsCustomerAccessException;
import com.liugh.rcs.service.RcsCustomerAccessService;
import com.liugh.rcs.service.RcsFileService;

@RestController
public class RcsFileResourceController {

    private final RcsCustomerAccessService accessService;
    private final RcsFileService fileService;

    @Autowired
    public RcsFileResourceController(RcsCustomerAccessService accessService,
            RcsFileService fileService) {
        this.accessService = accessService;
        this.fileService = fileService;
    }

    @GetMapping("/rcs/files/{uid}/metadata")
    @Pass
    public ResponseModel<JSONObject> get(
            @PathVariable("uid") String uid,
            @RequestParam(value = "Token", required = false) String upperToken,
            @RequestParam(value = "token", required = false) String lowerToken) {
        try {
            accessService.requirePhase1Enabled();
            AdminInfo customer = accessService.requireCustomer(
                    RcsControllerResponseHelper.auth(upperToken, lowerToken));
            return ResponseHelper.buildSuccessfulResponseModel(
                    view(fileService.getOwned(customer.getSsid(), uid)));
        } catch (RcsCustomerAccessException e) {
            return RcsControllerResponseHelper.failure(e);
        }
    }

    @PostMapping("/rcs/files/{uid}/delete")
    @Pass
    public ResponseModel<JSONObject> delete(
            @PathVariable("uid") String uid,
            @RequestParam(value = "Token", required = false) String upperToken,
            @RequestParam(value = "token", required = false) String lowerToken)
            throws Exception {
        try {
            accessService.requirePhase1Enabled();
            AdminInfo customer = accessService.requireCustomer(
                    RcsControllerResponseHelper.auth(upperToken, lowerToken));
            fileService.deleteOwned(customer.getSsid(), uid);
            JSONObject result = new JSONObject();
            result.put("name", "files/" + uid);
            result.put("deleted", true);
            return ResponseHelper.buildSuccessfulResponseModel(result);
        } catch (RcsCustomerAccessException e) {
            return RcsControllerResponseHelper.failure(e);
        }
    }

    private JSONObject view(RcsFile file) {
        JSONObject result = new JSONObject();
        result.put("name", "files/" + file.getUid());
        result.put("fileUri", file.getFileUri());
        result.put("thumbnailUri", file.getThumbnailUri());
        result.put("mimeType", file.getMimeType());
        result.put("fileSize", file.getFileSize());
        result.put("fileName", file.getFileName());
        result.put("createdAt", file.getCreateTime());
        return result;
    }
}
