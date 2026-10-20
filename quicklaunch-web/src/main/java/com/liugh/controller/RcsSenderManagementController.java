package com.liugh.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.liugh.annotation.Pass;
import com.liugh.config.ResponseHelper;
import com.liugh.config.ResponseModel;
import com.liugh.rcs.service.RcsCustomerAccessException;
import com.liugh.rcs.service.RcsSenderManagementService;

/**
 * Unified sender-management APIs. The contract authenticates with Token/token
 * and deliberately has no channel-selection parameter.
 */
@RestController
public class RcsSenderManagementController {

    private final RcsSenderManagementService service;

    @Autowired
    public RcsSenderManagementController(RcsSenderManagementService service) {
        this.service = service;
    }

    @PostMapping("/rcs/senders/create")
    @Pass
    public ResponseModel<JSONObject> createSender(
            @RequestBody final JSONObject request) {
        return respond(new Action() {
            @Override public JSONObject execute() {
                return service.createSender(request);
            }
        });
    }

    @GetMapping("/rcs/senders/list")
    @Pass
    public ResponseModel<JSONObject> listSenders(
            @RequestParam(value = "Token", required = false) String upperToken,
            @RequestParam(value = "token", required = false) String lowerToken) {
        final JSONObject auth = auth(upperToken, lowerToken);
        return respond(new Action() {
            @Override public JSONObject execute() {
                return service.listSenders(auth);
            }
        });
    }

    @GetMapping("/rcs/senders/{senderId}/detail")
    @Pass
    public ResponseModel<JSONObject> getSender(
            @PathVariable("senderId") final String senderId,
            @RequestParam(value = "Token", required = false) String upperToken,
            @RequestParam(value = "token", required = false) String lowerToken) {
        final JSONObject auth = auth(upperToken, lowerToken);
        return respond(new Action() {
            @Override public JSONObject execute() {
                return service.getSender(auth, senderId);
            }
        });
    }

    @PostMapping("/rcs/senders/{senderId}/update")
    @Pass
    public ResponseModel<JSONObject> updateSender(
            @PathVariable("senderId") final String senderId,
            @RequestBody final JSONObject request) {
        return respond(new Action() {
            @Override public JSONObject execute() {
                return service.updateSender(request, senderId);
            }
        });
    }

    @GetMapping("/rcs/senders/{senderId}/status")
    @Pass
    public ResponseModel<JSONObject> getSenderStatus(
            @PathVariable("senderId") final String senderId,
            @RequestParam(value = "Token", required = false) String upperToken,
            @RequestParam(value = "token", required = false) String lowerToken) {
        final JSONObject auth = auth(upperToken, lowerToken);
        return respond(new Action() {
            @Override public JSONObject execute() {
                return service.getSenderStatus(auth, senderId);
            }
        });
    }

    @PostMapping("/rcs/senders/{senderId}/platform-parameters/update")
    @Pass
    public ResponseModel<JSONObject> updateSenderPlatformParameters(
            @PathVariable("senderId") final String senderId,
            @RequestBody final JSONObject request) {
        return respond(new Action() {
            @Override public JSONObject execute() {
                return service.updateSenderPlatformParameters(request,
                        senderId);
            }
        });
    }

    @GetMapping("/rcs/providers")
    @Pass
    public ResponseModel<JSONObject> getProviders(
            @RequestParam("country") final String country,
            @RequestParam(value = "Token", required = false) String upperToken,
            @RequestParam(value = "token", required = false) String lowerToken) {
        final JSONObject auth = auth(upperToken, lowerToken);
        return respond(new Action() {
            @Override public JSONObject execute() {
                return service.getProviders(auth, country);
            }
        });
    }

    @PostMapping("/rcs/senders/{senderId}/test-numbers/add")
    @Pass
    public ResponseModel<JSONObject> addTestNumber(
            @PathVariable("senderId") final String senderId,
            @RequestBody final JSONObject request) {
        return respond(new Action() {
            @Override public JSONObject execute() {
                return service.addTestNumber(request, senderId);
            }
        });
    }

    @GetMapping("/rcs/senders/{senderId}/test-numbers/list")
    @Pass
    public ResponseModel<JSONObject> listTestNumbers(
            @PathVariable("senderId") final String senderId,
            @RequestParam(value = "Token", required = false) String upperToken,
            @RequestParam(value = "token", required = false) String lowerToken) {
        final JSONObject auth = auth(upperToken, lowerToken);
        return respond(new Action() {
            @Override public JSONObject execute() {
                return service.listTestNumbers(auth, senderId);
            }
        });
    }

    @GetMapping("/rcs/senders/{senderId}/test-numbers/{number}")
    @Pass
    public ResponseModel<JSONObject> getTestNumber(
            @PathVariable("senderId") final String senderId,
            @PathVariable("number") final String number,
            @RequestParam(value = "Token", required = false) String upperToken,
            @RequestParam(value = "token", required = false) String lowerToken) {
        final JSONObject auth = auth(upperToken, lowerToken);
        return respond(new Action() {
            @Override public JSONObject execute() {
                return service.getTestNumber(auth, senderId, number);
            }
        });
    }

    @PostMapping("/rcs/senders/{senderId}/test-numbers/{number}/refresh")
    @Pass
    public ResponseModel<JSONObject> refreshTestNumber(
            @PathVariable("senderId") final String senderId,
            @PathVariable("number") final String number,
            @RequestBody final JSONObject request) {
        return respond(new Action() {
            @Override public JSONObject execute() {
                return service.refreshTestNumber(request, senderId, number);
            }
        });
    }

    @PostMapping("/rcs/senders/{senderId}/test-numbers/{number}/primary")
    @Pass
    public ResponseModel<JSONObject> setPrimaryTestNumber(
            @PathVariable("senderId") final String senderId,
            @PathVariable("number") final String number,
            @RequestBody final JSONObject request) {
        return respond(new Action() {
            @Override public JSONObject execute() {
                return service.setPrimaryTestNumber(request, senderId, number);
            }
        });
    }

    @PostMapping("/rcs/senders/{senderId}/test-numbers/{number}/request-status")
    @Pass
    public ResponseModel<JSONObject> getTestNumberRequestStatus(
            @PathVariable("senderId") final String senderId,
            @PathVariable("number") final String number,
            @RequestBody final JSONObject request) {
        return respond(new Action() {
            @Override public JSONObject execute() {
                return service.getTestNumberRequestStatus(request, senderId,
                        number);
            }
        });
    }

    @PostMapping("/rcs/senders/{senderId}/templates/create")
    @Pass
    public ResponseModel<JSONObject> createTemplate(
            @PathVariable("senderId") final String senderId,
            @RequestBody final JSONObject request) {
        return respond(new Action() {
            @Override public JSONObject execute() {
                return service.createTemplate(request, senderId);
            }
        });
    }

    @GetMapping("/rcs/senders/{senderId}/templates/list")
    @Pass
    public ResponseModel<JSONObject> listTemplates(
            @PathVariable("senderId") final String senderId,
            @RequestParam(value = "Token", required = false) String upperToken,
            @RequestParam(value = "token", required = false) String lowerToken) {
        final JSONObject auth = auth(upperToken, lowerToken);
        return respond(new Action() {
            @Override public JSONObject execute() {
                return service.listTemplates(auth, senderId);
            }
        });
    }

    @GetMapping("/rcs/senders/{senderId}/templates/{templateId}/detail")
    @Pass
    public ResponseModel<JSONObject> getTemplate(
            @PathVariable("senderId") final String senderId,
            @PathVariable("templateId") final String templateId,
            @RequestParam(value = "Token", required = false) String upperToken,
            @RequestParam(value = "token", required = false) String lowerToken) {
        final JSONObject auth = auth(upperToken, lowerToken);
        return respond(new Action() {
            @Override public JSONObject execute() {
                return service.getTemplate(auth, senderId, templateId);
            }
        });
    }

    @PostMapping("/rcs/senders/{senderId}/templates/{templateId}/update")
    @Pass
    public ResponseModel<JSONObject> updateTemplate(
            @PathVariable("senderId") final String senderId,
            @PathVariable("templateId") final String templateId,
            @RequestBody final JSONObject request) {
        return respond(new Action() {
            @Override public JSONObject execute() {
                return service.updateTemplate(request, senderId, templateId);
            }
        });
    }

    @PostMapping("/rcs/senders/{senderId}/templates/{templateId}/register")
    @Pass
    public ResponseModel<JSONObject> registerTemplate(
            @PathVariable("senderId") final String senderId,
            @PathVariable("templateId") final String templateId,
            @RequestBody final JSONObject request) {
        return respond(new Action() {
            @Override public JSONObject execute() {
                return service.registerTemplate(request, senderId, templateId);
            }
        });
    }

    @PostMapping("/rcs/senders/{senderId}/templates/{templateId}/delete")
    @Pass
    public ResponseModel<JSONObject> deleteTemplate(
            @PathVariable("senderId") final String senderId,
            @PathVariable("templateId") final String templateId,
            @RequestParam(value = "Token", required = false) String upperToken,
            @RequestParam(value = "token", required = false) String lowerToken) {
        final JSONObject auth = auth(upperToken, lowerToken);
        return respond(new Action() {
            @Override public JSONObject execute() {
                return service.deleteTemplate(auth, senderId, templateId);
            }
        });
    }

    @GetMapping("/rcs/senders/{senderId}/templates/{templateId}/status")
    @Pass
    public ResponseModel<JSONObject> getTemplateStatus(
            @PathVariable("senderId") final String senderId,
            @PathVariable("templateId") final String templateId,
            @RequestParam(value = "Token", required = false) String upperToken,
            @RequestParam(value = "token", required = false) String lowerToken) {
        final JSONObject auth = auth(upperToken, lowerToken);
        return respond(new Action() {
            @Override public JSONObject execute() {
                return service.getTemplateStatus(auth, senderId, templateId);
            }
        });
    }

    @GetMapping("/rcs/senders/{senderId}/launch-status")
    @Pass
    public ResponseModel<JSONObject> getLaunchStatus(
            @PathVariable("senderId") final String senderId,
            @RequestParam(value = "Token", required = false) String upperToken,
            @RequestParam(value = "token", required = false) String lowerToken) {
        final JSONObject auth = auth(upperToken, lowerToken);
        return respond(new Action() {
            @Override public JSONObject execute() {
                return service.getLaunchStatus(auth, senderId);
            }
        });
    }

    private ResponseModel<JSONObject> respond(Action action) {
        try {
            return ResponseHelper.buildSuccessfulResponseModel(action.execute());
        } catch (RcsCustomerAccessException e) {
            return RcsControllerResponseHelper.failure(e);
        }
    }

    private static JSONObject auth(String upperToken, String lowerToken) {
        return RcsControllerResponseHelper.auth(upperToken, lowerToken);
    }

    private interface Action {
        JSONObject execute();
    }
}
