package com.bolarde.oauth2login.Controller;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bolarde.oauth2login.Service.GoogleContactsService;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
@RequestMapping("/contacts")
public class ContactsController {
    @Autowired
    private GoogleContactsService googleContactsService;

    @GetMapping("/getContacts")
    public String getContacts(@AuthenticationPrincipal OAuth2User user, Model model) {
        List<Map<String, Object>> contacts = googleContactsService.getContacts(user);
        Map<String, Object> userProfile = googleContactsService.getUserProfile(user);
        String profilePicUrl = "default-profile.png";
        if (userProfile != null && userProfile.get("photos") != null) {
            List<Map<String, Object>> photos = (List<Map<String, Object>>) userProfile.get("photos");
            if (!photos.isEmpty()) {
                profilePicUrl = (String) photos.get(0).get("url");
            }
        }
        model.addAttribute("contacts", contacts);
        model.addAttribute("profilePic", profilePicUrl);
        return "contacts"; 
    }

    @GetMapping("/create-contact")
    public String getCreateContactForm() {
        return "create-contact"; 
    }

    @PostMapping("/createContact")
    @ResponseBody
    public Map<String, Object> createContact(
            @AuthenticationPrincipal OAuth2User user,
            @RequestBody Map<String, String> contactData) {

        String name = contactData.get("name");
        String email = contactData.get("email");
        String phone = contactData.get("phone");
        String birthday = contactData.get("birthday");
        String address = contactData.get("address");
        String photoUrl = contactData.get("photoUrl");
        
        // System.out.println("name: " + name);
        // System.out.println("email: " + email);
        // System.out.println("phone: " + phone);
        // System.out.println("birthday: " + birthday);
        // System.out.println("address: " + address);
        // System.out.println("photoUrl: " + photoUrl);

        return googleContactsService.createContact(user, name, email, phone, birthday, address, photoUrl);
    }

   @DeleteMapping("/delete")
    public ResponseEntity<String> deleteContact(@AuthenticationPrincipal OAuth2User user,
                                                @RequestParam String contactId) {
        try {
            System.out.println("Received contact ID: " + contactId);
            googleContactsService.deleteContact(user, contactId);
            return ResponseEntity.ok("Contact deleted successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid contact ID.");
        }
    }
    @PostMapping("/updateContact")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateContact(
            @AuthenticationPrincipal OAuth2User user,
            @RequestBody Map<String, String> contactData) {

        System.out.println("Received contact data: " + contactData);
        String resourceName = contactData.get("resourceName");
        String name = contactData.get("name");
        String email = contactData.get("email");
        String phone = contactData.get("phone");
        String birthday = contactData.get("birthday");
        String address = contactData.get("address");

        if (resourceName == null || resourceName.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid resource name"));
        }

        Map<String, Object> updatedContact = googleContactsService.updateContact(user, resourceName, name, email, phone, birthday, address);
        
        return ResponseEntity.ok(updatedContact);
    }
    
}
