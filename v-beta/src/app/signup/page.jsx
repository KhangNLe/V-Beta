import { SignupForm } from "@/components/ui/signup-form"
import { AuthBrandedShell } from "@/components/auth-branded-shell"

export default function SignupPage() {
  return (
    <AuthBrandedShell>
      <SignupForm />
    </AuthBrandedShell>
  )
}
