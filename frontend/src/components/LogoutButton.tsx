import Button from "./Button";
import { useNavigate } from "react-router-dom";
import {handleLogout} from "../api/user";

export default function LogoutButton(){
    const navigate = useNavigate();
    return(
        <Button onClick={() => handleLogout(localStorage.getItem("accessToken"), navigate)}>
          Logout
        </Button>
    )
}