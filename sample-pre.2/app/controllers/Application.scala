package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {
  
  def pre2 = Action {
    Ok(views.html.pre2())
  }
  
}