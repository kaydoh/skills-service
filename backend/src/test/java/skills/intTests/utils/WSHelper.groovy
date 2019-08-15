package skills.intTests.utils

import com.fasterxml.jackson.annotation.JsonProperty
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.springframework.core.io.FileSystemResource
import org.springframework.http.*
import org.springframework.http.client.support.BasicAuthenticationInterceptor
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate

@Slf4j
class WSHelper {

    String skillsService = "http://localhost:8080"
    RestTemplateWrapper restTemplateWrapper
    RestTemplate oAuthRestTemplate
    JsonSlurper jsonSlurper = new JsonSlurper()

    String username = "skills@skills.org"
    String password = "p@ssw0rd"

    WSHelper init() {
        restTemplateWrapper = new RestTemplateWrapper()
        oAuthRestTemplate = new RestTemplate()
        restTemplateWrapper.auth(skillsService, username, password)
        return this
    }

    void setProxyCredentials(String clientId, String secretCode) {
        oAuthRestTemplate.setInterceptors([new BasicAuthenticationInterceptor(clientId, secretCode)])
    }

    def appPut(String endpoint, def params) {
        put(endpoint, "app", params)
    }

    def appPost(String endpoint, def params) {
        post(endpoint, "app", params)
    }

    def appGet(String endpoint, Map params = null) {
        return get(endpoint, "app", params)
    }

    def rootContextGet(String endpoint, Map params = null) {
        return get(endpoint, "", params)
    }

    def adminPut(String endpoint, def params = null) {
        put(endpoint, "admin", params)
    }

    def adminPost(String endpoint, def params, boolean throwExceptionOnFailure = true) {
        post(endpoint, "admin", params, HttpStatus.OK, throwExceptionOnFailure )
    }

    def adminDelete(String endpoint, def params = null) {
        String url = "${skillsService}/admin${endpoint}${getUrlFromParams(params)}"
        delete(endpoint, "admin", params)
    }

    def adminGet(String endpoint, Map params = null) {
        return get(endpoint, "admin", params)
    }

    def adminUpload(String endpoint, Map params = null) {
        String url = "${skillsService}/admin${endpoint}"
        log.info("MULTIPART POST: {}", url)
        return multipartPost(url, params)
    }

    def adminPatch(String endpoint, def params, boolean throwExceptionOnFailure = true, MediaType mediaType = MediaType.APPLICATION_JSON) {
        patch(endpoint, "admin", params, HttpStatus.OK, throwExceptionOnFailure, mediaType)
    }

    def rootGet(String endpoint) {
        return get(endpoint, 'root', null)
    }

    def rootPut(String endpoint, Map params = null) {
        return put(endpoint, 'root', params)
    }

    def createRootAccount(Map<String, String> userInfo) {
        return put('/createRootAccount', '', userInfo)
    }

    def grantRoot() {
        return post('/grantFirstRoot', '', null)
    }

    def serverPut(String endpoint, def params) {
        put(endpoint, "server", params)
    }

    def serverPost(String endpoint, def params) {
       return post(endpoint, "server", params)
    }

    def proxyApiGet(String token, String endpoint, def params=null) {
        this.restTemplateWrapper.authenticationToken = "Bearer ${token}"
        def result = get(endpoint, "api", params)
        this.restTemplateWrapper.authenticationToken = null
        return result
    }

    def proxyApiPost(String token, String endpoint, def params) {
        this.restTemplateWrapper.authenticationToken = "Bearer ${token}"
        def result = post(endpoint, "api", params)
        this.restTemplateWrapper.authenticationToken = null
        return result
    }

    def proxyApiPut(String token, String endpoint, def params) {
        this.restTemplateWrapper.authenticationToken = "Bearer ${token}"
        def result = put(endpoint, "api", params)
        this.restTemplateWrapper.authenticationToken = null
        return result
    }

    def apiPost(String endpoint, def params) {
        return post(endpoint, "api", params)
    }

    def apiPut(String endpoint, def params) {
        return put(endpoint, "api", params)
    }

    def apiGet(String endpoint, Map params = null) {
        return get(endpoint, "api", params)
    }

    def iconsGetImage(String endpoint, def params = null){
        String url = "${skillsService}/icons${endpoint}${getUrlFromParams(params)}"
        log.info("GET: {}", url)
        ResponseEntity<byte[]> responseEntity = restTemplateWrapper.getForEntity(url, byte[].class)

        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            return responseEntity.getBody()
        }else{
            log.warn("unable to retrieve icon from ${url}, status code is ${responseEntity.getStatusCode()}")
            return null
        }
    }

    def iconsGet(String endpoint, Map params = null) {
        String url = "${skillsService}/icons${endpoint}${getUrlFromParams(params)}"
        log.info("GET: {}", url)

        ResponseEntity<String> responseEntity = restTemplateWrapper.getForEntity(url, String)
        return responseEntity.getBody()
    }

    String getTokenForUser(String userId) {
        log.info("Getting token for user [$userId]")
        String tokenUrl = "${skillsService}/oauth/token"
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED)

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>()
        body.add('grant_type', 'client_credentials')
        body.add('proxy_user', userId)

        ResponseEntity<OAuth2Response> responseEntity = oAuthRestTemplate.postForEntity(tokenUrl, new HttpEntity<>(body, headers), OAuth2Response)

        return responseEntity.body.accessToken
    }

    private def put(String endpoint, String type, def params, HttpStatus expectedStatus = HttpStatus.OK, boolean throwExceptionOnFailure = true) {
        String url = "${skillsService}/${type}${endpoint}"
        log.info("PUT: {}, params={}", url, params)
        ResponseEntity<String> responseEntity = restTemplateWrapper.putForEntity(url, params)
        return getResultFromEntity(url, responseEntity, expectedStatus, throwExceptionOnFailure)
    }

    private def post(String endpoint, String type, def params, HttpStatus expectedStatus = HttpStatus.OK, boolean throwExceptionOnFailure = true) {
        String url = "${skillsService}/${type}${endpoint}".toString()
        log.info("POST: {}, params={}", url, params)
        ResponseEntity<String> responseEntity = restTemplateWrapper.postForEntity(url, params, String)
        return getResultFromEntity(url, responseEntity, expectedStatus, throwExceptionOnFailure)
    }

    private def patch(String endpoint, String type, def params, HttpStatus expectedStatus = HttpStatus.OK, boolean throwExceptionOnFailure = true, MediaType mediaType= MediaType.APPLICATION_JSON) {
        String url = "${skillsService}/${type}${endpoint}".toString()
        log.info("PATCH: {}, params={}", url, params)
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(mediaType)
        HttpEntity entity = new HttpEntity(params, headers)
        ResponseEntity<String> responseEntity = restTemplateWrapper.patchForObject(url, entity, String)
        return getResultFromEntity(url, responseEntity, expectedStatus, throwExceptionOnFailure)
    }

    private def delete(String endpoint, String type, def params, HttpStatus expectedStatus = HttpStatus.OK, boolean throwExceptionOnFailure = true) {
        String url = "${skillsService}/${type}${endpoint}".toString()
        log.info("DELETE: {}, params={}", url, params)
        ResponseEntity<String> responseEntity = restTemplateWrapper.deleteForEntity(url, params, String)
        return getResultFromEntity(url, responseEntity, expectedStatus, throwExceptionOnFailure)
    }

    private def getResultFromEntity(String url, ResponseEntity<String> responseEntity, HttpStatus expectedStatus, boolean throwExceptionOnFailure) {

        String resBody = responseEntity.body
//        if(!resBody){
//            throw new IllegalArgumentException("Bad request for [$url], params=$params, code=${responseEntity.statusCode}")
//        }

        def res = ['statusCode': responseEntity.statusCode]
        if (responseEntity.statusCode.is2xxSuccessful()) {
            if(resBody) {
                log.info("  Result:\n {}", JsonOutput.prettyPrint(resBody))
                res['body'] = jsonSlurper.parseText(resBody)
            }else{
                log.info("request was successful but empty response body returned")
            }
            res['success'] = true
        } else {
            try {
                res['body'] = jsonSlurper.parseText(resBody)
            } catch (Exception e) {
                res['body'] = resBody
            }

            if (throwExceptionOnFailure) {
                String msg = "Bad request for [$url]. Res: ${res['body']} code=${responseEntity.statusCode}"
                log.error(msg)
                throw new SkillsClientException(msg, url, responseEntity.statusCode)
//                throw new IllegalStateException("Request [$url] failed. Res: ${res['body']}".toString())
            }

            res['success'] = false
        }
        if (responseEntity.statusCode != expectedStatus) {
            log.error('Failed with {} code. {}', responseEntity.statusCode, res)
        }

        return res
    }

    private def get(String endpoint, String type, def params, boolean isResJson = true) {
        String url = "${skillsService}/${type}${endpoint}${getUrlFromParams(params)}"
        log.info("GET: {}", url)
        ResponseEntity<String> responseEntity = restTemplateWrapper.getForEntity(url, String)
        String resBody = responseEntity.body
        if(!resBody || responseEntity.statusCode != HttpStatus.OK){
            String msg = "Bad request for [$url] code=${responseEntity.statusCode}"
            log.error(msg)
             throw new SkillsClientException(msg, url, responseEntity.statusCode)
        }
        def res = resBody
        if (isResJson) {
            log.info("  Result:\n {}", JsonOutput.prettyPrint(resBody))
            res = jsonSlurper.parseText(resBody)
        }
        return res
    }

    private def multipartPost(String endpoint, Map params){
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.MULTIPART_FORM_DATA)

        //TEMP



        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>()
        params.each { key, val ->
            body.add(key.toString(), new FileSystemResource(val))
        }

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers)

        ResponseEntity<String> responseEntity = restTemplateWrapper.postForEntity(endpoint, requestEntity, String)
        String resBody = responseEntity.body

        log.info("  Result:\n {}", JsonOutput.prettyPrint(resBody))

        def res = jsonSlurper.parseText(resBody)
        if(responseEntity.statusCode == HttpStatus.OK) {
            res['success' ] = true
        } else {
            res['success' ] = false
        }

        return res
    }

    private String getUrlFromParams(Map params) {
        String url = ""
        if (params) {
            url += "?"
            url += params.entrySet().collect({ "${it.key}=${it.value}" }).join(",")
        }
        return url
    }

    static class OAuth2Response {
        @JsonProperty("access_token")
        String accessToken
        @JsonProperty("token_type")
        String tokenType
        @JsonProperty("expires_in")
        Long expiresIn
        String scope
        @JsonProperty("proxy_user")
        String proxyUser
        String jti
    }
}