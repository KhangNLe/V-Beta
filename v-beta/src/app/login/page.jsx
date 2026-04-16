import { LoginForm } from "@/components/ui/login-form"
import { AuthBrandedShell } from "@/components/auth-branded-shell"
import { GuestRouteGuard } from "@/components/guest-route-guard"

export default function LoginPage() {
  return (
    <AuthBrandedShell>
      <GuestRouteGuard>
        <LoginForm />
      </GuestRouteGuard>
    </AuthBrandedShell>
  )
}
