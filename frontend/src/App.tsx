import { RouterProvider } from "react-router-dom";
import { router } from "./router";
import { DevRoleSwitcher } from "./components/dev/DevRoleSwitcher";

function App() {
    return (
        <>
            <RouterProvider router={router} />
            <DevRoleSwitcher />
        </>
    );
}

export default App;
