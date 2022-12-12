<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateMFeedbackTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('m_feedback', function (Blueprint $table) {
            $table->increments('F_id');

            $table->string('F_Name', 100)->nullable();
            $table->string('F_Family', 100)->nullable();
            $table->string('F_Phone', 100)->nullable();
            $table->string('F_Mail', 100)->nullable();
            $table->string('F_Request', 600)->nullable();



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
        Schema::dropIfExists('m_feedback');
    }
}
