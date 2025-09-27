package com.ratewise.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

  // Add every client-side route here so hard refresh works
  @GetMapping({"/", "/home", "/calculator", "/tariffs", "/login", "/profile"})
  public String index() {
    return "forward:/index.html";
  }
}
