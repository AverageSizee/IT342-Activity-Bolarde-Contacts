package com.bolarde.oauth2login.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GoogleContactsService {

    private static final String GOOGLE_CONTACTS_API_URL =
    "https://people.googleapis.com/v1/people/me/connections"
           + "?personFields=names,emailAddresses,phoneNumbers,birthdays,addresses,photos";

    private static final String GOOGLE_CREATE_CONTACTS_API_URL = "https://people.googleapis.com/v1/people:createContact";

    private final RestTemplate restTemplate;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public GoogleContactsService(RestTemplate restTemplate, OAuth2AuthorizedClientService authorizedClientService) {
        this.restTemplate = restTemplate;
        this.authorizedClientService = authorizedClientService;
    }

    public List<Map<String, Object>> getContacts(OAuth2User user) {
        // Retrieve the authorized client for Google
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                "google", user.getName());

        if (client == null || client.getAccessToken() == null) {
            throw new RuntimeException("Access token is missing!");
        }

        String accessToken = client.getAccessToken().getTokenValue();

        // Set up headers
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Make API request
        ResponseEntity<Map> response = restTemplate.exchange(
                GOOGLE_CONTACTS_API_URL, HttpMethod.GET, entity, Map.class
        );

        // Extract and return contacts
        Map<String, Object> responseBody = response.getBody();
        //System.out.println("Contacts List: " + response.getBody());
        return (List<Map<String, Object>>) responseBody.getOrDefault("connections", new ArrayList<>());
        
    }

    public Map<String, Object> getUserProfile(OAuth2User user) {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                "google", user.getName());
    
        if (client == null || client.getAccessToken() == null) {
            throw new RuntimeException("Access token is missing!");
        }
    
        String accessToken = client.getAccessToken().getTokenValue();
    
        // Google People API: Fetch profile information
        String url = "https://people.googleapis.com/v1/people/me?personFields=names,photos";
    
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
    
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        return response.getBody();
    }

    public Map<String, Object> createContact(OAuth2User user, String name, String email, String phone, 
        String birthday, String address, String photoBase64) {
        
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("google", user.getName());

        if (client == null || client.getAccessToken() == null) {
            throw new RuntimeException("Access token is missing!");
        }

        String accessToken = client.getAccessToken().getTokenValue();

        Map<String, Object> contactData = new HashMap<>();

        if (name != null) {
            Map<String, String> nameMap = new HashMap<>();
            nameMap.put("givenName", name);
            contactData.put("names", new Object[]{ nameMap });
        }

        if (email != null) {
            Map<String, String> emailMap = new HashMap<>();
            emailMap.put("value", email);
            contactData.put("emailAddresses", new Object[]{ emailMap });
        }

        if (phone != null) {
            Map<String, String> phoneMap = new HashMap<>();
            phoneMap.put("value", phone);
            contactData.put("phoneNumbers", new Object[]{ phoneMap });
        }

        if (address != null) {
            Map<String, String> addressMap = new HashMap<>();
            addressMap.put("formattedValue", address);
            contactData.put("addresses", new Object[]{ addressMap });
        }

        // Convert birthday to proper format
        if (birthday != null && !birthday.isEmpty()) {
            String[] parts = birthday.split("-");
            if (parts.length == 3) {
                Map<String, Integer> dateMap = new HashMap<>();
                dateMap.put("year", Integer.parseInt(parts[0]));
                dateMap.put("month", Integer.parseInt(parts[1]));
                dateMap.put("day", Integer.parseInt(parts[2]));

                Map<String, Object> birthdayMap = new HashMap<>();
                birthdayMap.put("date", dateMap);

                contactData.put("birthdays", new Object[]{ birthdayMap });
            }
        }

        // Convert Base64 image to a Google-compatible format
        if (photoBase64 != null && !photoBase64.isEmpty()) {
            Map<String, String> photoMap = new HashMap<>();
            photoMap.put("photoUrl", "data:image/png;base64," + photoBase64);
            contactData.put("photos", new Object[]{ photoMap });
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Content-Type", "application/json");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(contactData, headers);
        ResponseEntity<Map> response = restTemplate.exchange(GOOGLE_CREATE_CONTACTS_API_URL, HttpMethod.POST, entity, Map.class);

        return response.getBody();
    }

    public String deleteContact(OAuth2User user, String resourceName) {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("google", user.getName());
    
        if (client == null || client.getAccessToken() == null) {
            throw new RuntimeException("Access token is missing!");
        }
    
        String accessToken = client.getAccessToken().getTokenValue();
        String getUrl = "https://people.googleapis.com/v1/" + resourceName + "?personFields=names,emailAddresses,phoneNumbers";
    
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
    
        try {
            ResponseEntity<String> response = restTemplate.exchange(getUrl, HttpMethod.GET, entity, String.class);
            System.out.println("Contact details before delete: " + response.getBody());
        } catch (Exception e) {
            System.out.println("Contact does not exist or cannot be retrieved: " + resourceName);
            throw new RuntimeException("Contact does not exist!");
        }
    
        // Now attempt to delete
        String deleteUrl = "https://people.googleapis.com/v1/" + resourceName + ":deleteContact";
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(deleteUrl, HttpMethod.DELETE, entity, Void.class);
    
        if (deleteResponse.getStatusCode().is2xxSuccessful()) {
            return "Contact deleted successfully.";
        } else {
            throw new RuntimeException("Failed to delete contact.");
        }

    }

    public Map<String, Object> updateContact(OAuth2User user, String resourceName, String name, String email, String phone, String birthday, String address) {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("google", user.getName());
    
        if (client == null || client.getAccessToken() == null) {
            throw new RuntimeException("Access token is missing!");
        }
    
        String accessToken = client.getAccessToken().getTokenValue();
    
        // Step 1: Retrieve the existing contact to get the etag
        String getUrl = "https://people.googleapis.com/v1/" + resourceName + "?personFields=metadata,names,emailAddresses,phoneNumbers,addresses,birthdays";
        
        HttpHeaders getHeaders = new HttpHeaders();
        getHeaders.setBearerAuth(accessToken);
    
        HttpEntity<String> getEntity = new HttpEntity<>(getHeaders);
        ResponseEntity<Map> getResponse = restTemplate.exchange(getUrl, HttpMethod.GET, getEntity, Map.class);
    
        Map<String, Object> existingContact = getResponse.getBody();
        if (existingContact == null || !existingContact.containsKey("etag")) {
            throw new RuntimeException("Failed to retrieve etag for the contact.");
        }
    
        String etag = (String) existingContact.get("etag");
    
        // Step 2: Prepare the update request with etag
        Map<String, Object> contactData = new HashMap<>();
        contactData.put("etag", etag);  // Required for update
    
        if (name != null) {
            Map<String, String> nameMap = new HashMap<>();
            nameMap.put("givenName", name);
            contactData.put("names", new Object[]{ nameMap });
        }
    
        if (email != null) {
            Map<String, String> emailMap = new HashMap<>();
            emailMap.put("value", email);
            contactData.put("emailAddresses", new Object[]{ emailMap });
        }
    
        if (phone != null) {
            Map<String, String> phoneMap = new HashMap<>();
            phoneMap.put("value", phone);
            contactData.put("phoneNumbers", new Object[]{ phoneMap });
        }
    
        if (address != null) {
            Map<String, String> addressMap = new HashMap<>();
            addressMap.put("formattedValue", address);
            contactData.put("addresses", new Object[]{ addressMap });
        }
    
        if (birthday != null && !birthday.isEmpty()) {
            String[] parts = birthday.split("-");
            if (parts.length == 3) {
                Map<String, Integer> dateMap = new HashMap<>();
                dateMap.put("year", Integer.parseInt(parts[0]));
                dateMap.put("month", Integer.parseInt(parts[1]));
                dateMap.put("day", Integer.parseInt(parts[2]));
    
                Map<String, Object> birthdayMap = new HashMap<>();
                birthdayMap.put("date", dateMap);
    
                contactData.put("birthdays", new Object[]{ birthdayMap });
            }
        }
    
        HttpHeaders updateHeaders = new HttpHeaders();
        updateHeaders.setBearerAuth(accessToken);
        updateHeaders.setContentType(MediaType.APPLICATION_JSON);
        updateHeaders.set("X-HTTP-Method-Override", "PATCH"); // Required for Google API
    
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(contactData, updateHeaders);
    
        // Google API endpoint to update a contact
        String updateUrl = "https://people.googleapis.com/v1/" + resourceName + ":updateContact?updatePersonFields=names,emailAddresses,phoneNumbers,addresses,birthdays";
    
        ResponseEntity<Map> response = restTemplate.exchange(updateUrl, HttpMethod.POST, entity, Map.class);
    
        return response.getBody();
    }    
    
    
}
