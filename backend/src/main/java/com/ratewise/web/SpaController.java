package com.ratewise;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

  // Add every client-side route here so hard refresh or direct URL access works
  @GetMapping({
      "/", 
      "/home", 
      "/calculator", 
      "/tariffs", 
      "/login", 
      "/profile",
      "/oauth-callback"   // <-- add this line
  })
  public String index() {
    // Forward the request to index.html inside /static or /public
    return "forward:/index.html";
  }
}
