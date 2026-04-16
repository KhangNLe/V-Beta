import { SignupForm } from "@/components/ui/signup-form"
import { AuthBrandedShell } from "@/components/auth-branded-shell"
import { GuestRouteGuard } from "@/components/guest-route-guard"

export default function SignupPage() {
  return (
    <AuthBrandedShell>
      <GuestRouteGuard>
        <SignupForm />
      </GuestRouteGuard>
    </AuthBrandedShell>
  )
}
