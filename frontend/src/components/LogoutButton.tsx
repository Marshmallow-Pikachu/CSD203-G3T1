import Button from "./Button";
import { useNavigate } from "react-router-dom";


export default function LogoutButton(){

    const navigate = useNavigate();

    const handleLogout = () => {
    localStorage.removeItem("accessToken"); // Clear JWT
    navigate("/login"); // redirect to login
  };

    return(
        <form onSubmit={handleLogout}>
            <Button type="submit"> Logout </Button>
        </form>
    )
}