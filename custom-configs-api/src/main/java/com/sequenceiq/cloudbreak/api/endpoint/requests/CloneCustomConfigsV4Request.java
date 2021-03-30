package com.sequenceiq.cloudbreak.api.endpoint.requests;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloneCustomConfigsV4Request {

    @ApiModelProperty(value = ModelDescriptions.CustomConfigsModelDescription.CUSTOM_CONFIGS_NAME)
    @Size(min = 1, max = 100, message = "Length of custom configs name must be from 1 to 100 characters and shouldn't contain semicolon and percentage symbol")
    @Pattern(regexp = "^[^;\\/%]*$")
    @NotNull
    private String customConfigsName;

    @ApiModelProperty(value = ModelDescriptions.CustomConfigsModelDescription.VERSION)
    private String platformVersion;

    public String getCustomConfigsName() {
        return customConfigsName;
    }

    public void setCustomConfigsName(String customConfigsName) {
        this.customConfigsName = customConfigsName;
    }

    public String getPlatformVersion() {
        return platformVersion;
    }

    public void setPlatformVersion(String platformVersion) {
        this.platformVersion = platformVersion;
    }
}
