<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateRegClaimTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('reg_claim', function (Blueprint $table) {
            $table->increments('R_Cl_id');

            $table->string('R_Cl_key', 60)->index();
            $table->string('R_Cl_name', 190)->nullable();
            $table->text('R_Cl_data')->nullable();
            $table->boolean('R_Cl_submit')->nullable();
            $table->string('R_Cl_regNum', 60)->index()->nullable();


            $table->timestamps();
            $table->softDeletes();
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('reg_claim');
    }
}
